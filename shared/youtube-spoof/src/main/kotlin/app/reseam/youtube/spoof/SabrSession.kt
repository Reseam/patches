// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import android.os.SystemClock
import app.reseam.bundle.Logger
import app.reseam.youtube.core.Proto
import app.reseam.youtube.core.debug
import app.reseam.youtube.web.WebPlayer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val PROTECTION_OK = 1L
private const val ATTESTATION_PENDING = 2L
private const val ATTESTATION_REQUIRED = 3L
private const val REFRESH_TIMEOUT_MILLISECONDS = 30_000L
private val executor =
    ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue<Runnable>(50))

internal class SabrSession(val data: SabrData) {
    val contexts = SabrContexts()
    @Volatile var attestation = data.attestation
    @Volatile var rejectedSession: String? = null
    private var refreshing: CompletableFuture<WebPlayer.Attestation>? = null

    @Synchronized
    fun refresh(expectedSessionId: String?): CompletableFuture<WebPlayer.Attestation> {
        if (data.client != ClientType.WEB) return CompletableFuture.completedFuture(attestation)
        val currentSessionId = attestation.sessionId
        // Late rejections must not retire an already renewed minter.
        if (expectedSessionId != null && expectedSessionId != currentSessionId) {
            return CompletableFuture.completedFuture(attestation)
        }
        refreshing?.let {
            return it
        }
        val result = CompletableFuture<WebPlayer.Attestation>()
        refreshing = result
        try {
            executor.execute {
                try {
                    WebPlayer.open(SystemClock.elapsedRealtime() + REFRESH_TIMEOUT_MILLISECONDS)
                        .use { web ->
                            web.invalidateAttestation(currentSessionId)
                            val replacement = web.attest(data.videoId)
                            synchronized(this) { attestation = replacement }
                            Logger.debug { "SABR token refreshed for ${data.videoId}" }
                            result.complete(replacement)
                        }
                } catch (exception: Exception) {
                    result.completeExceptionally(exception)
                } finally {
                    synchronized(this) { if (refreshing === result) refreshing = null }
                }
            }
        } catch (exception: RuntimeException) {
            refreshing = null
            result.completeExceptionally(exception)
        }
        return result
    }

    fun control(type: Int, message: ByteArray, sessionId: String) {
        when (type) {
            RELOAD_PLAYER_RESPONSE ->
                Logger.debug { "SABR requested a player response reload for ${data.videoId}" }
            CONTEXT_UPDATE -> contexts.update(message)
            CONTEXT_SENDING_POLICY -> contexts.policy(message)
            STREAM_PROTECTION_STATUS -> {
                val status = Proto.number(message, 1, 0)
                synchronized(this) {
                    when (status) {
                        ATTESTATION_PENDING,
                        ATTESTATION_REQUIRED -> {
                            if (attestation.sessionId == sessionId) rejectedSession = sessionId
                        }
                        PROTECTION_OK -> {
                            if (sessionId == rejectedSession) rejectedSession = null
                        }
                    }
                }
                Logger.debug { "SABR protection status $status for ${data.videoId}" }
            }
        }
    }
}
