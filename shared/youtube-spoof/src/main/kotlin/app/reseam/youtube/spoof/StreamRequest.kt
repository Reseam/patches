// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import android.os.SystemClock
import app.reseam.bundle.Logger
import app.reseam.youtube.core.debug
import app.reseam.youtube.web.WebPlayer
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.FutureTask
import java.util.concurrent.ScheduledFuture

internal class StreamRequest(
    val videoId: String,
    val headers: Map<String, String>,
    private val config: StreamConfiguration,
    val generation: Long,
    val reloadToken: String?,
    private val offline: Boolean,
) {
    val deadline = SystemClock.elapsedRealtime() + StreamingDataRequest.REQUEST_TIMEOUT_MILLISECONDS
    @Volatile private var completed: StreamingData? = null
    private val http = StreamRequestHttp(headers, offline)
    @Volatile var timeout: ScheduledFuture<*>? = null
    val task: FutureTask<StreamingData?> =
        object : FutureTask<StreamingData?>({ fetch().also { completed = it } }) {
            override fun done() {
                timeout?.cancel(false)
            }
        }

    val isExpired: Boolean
        get() {
            val now = SystemClock.elapsedRealtime()
            return if (!task.isDone) deadline <= now
            else task.isCancelled || completed?.isFresh != true
        }

    fun cancel() {
        task.cancel(true)
        StreamingDataRequest.executor.remove(task)
        http.cancel()
    }

    private fun fetch(): StreamingData? {
        val visitor = headers[StreamingDataRequest.VISITOR_ID_HEADER]
        for ((index, client) in config.clients.withIndex()) {
            when {
                offline && client.sabr -> continue
                client == ClientType.TV_SIMPLY && visitor.isNullOrEmpty() -> continue
                client == ClientType.TV && StreamingDataRequest.authorization == null -> {
                    Logger.info { "Skipping TV without a signed-in account" }
                    continue
                }
            }
            if (client.native) {
                Logger.debug { "Native playback with a server integrity token for $videoId" }
                return StreamingData(client)
            }
            // The preferred client is also the slowest to start (web runtime, TV profile), so each attempt gets half
            // of the remaining time and the last gets all of it.
            val share = if (index == config.clients.lastIndex) 1 else 2
            val attemptDeadline = SystemClock.elapsedRealtime() + remaining(deadline) / share
            try {
                attempt(client, visitor, attemptDeadline)?.let {
                    return it
                }
            } catch (exception: InterruptedException) {
                Thread.currentThread().interrupt()
                throw exception
            } catch (exception: Exception) {
                Logger.info(exception) { "Spoof client $client failed for $videoId" }
            }
        }
        Logger.error { "No compatible client streams for $videoId" }
        return null
    }

    private fun attempt(
        client: ClientType,
        visitor: String?,
        attemptDeadline: Long,
    ): StreamingData? {
        return (if (client.usesWebPlayer) WebPlayer.open(attemptDeadline) else null).use { web ->
            val attestation =
                when (client) {
                    ClientType.WEB -> checkNotNull(web).attest(videoId)
                    ClientType.TV ->
                        WebPlayer.Attestation(
                            TvProfile.get(remaining(attemptDeadline)),
                            "",
                            Long.MAX_VALUE,
                            UUID.randomUUID().toString(),
                        )
                    else -> null
                }
            val locale = localeFor(client)
            attestation?.client?.apply {
                put("hl", locale.language)
                if (locale.country.isNotEmpty()) put("gl", locale.country)
            }
            val response = send(client, visitor, web, attestation, attemptDeadline)
            remaining(attemptDeadline)
            val parsed = parsePlayerResponse(response)
            val streamingData = parsed.streamingData
            if (parsed.status != PLAYER_STATUS_OK || streamingData == null) {
                Logger.info {
                    "Spoof client $client has no playable response, status ${parsed.status}" +
                        (parsed.reason?.let { ": $it" } ?: "")
                }
                return null
            }
            if (client.sabr) {
                val sabr =
                    resolveSabrData(
                        videoId,
                        client,
                        parsed,
                        checkNotNull(web),
                        checkNotNull(attestation),
                    )
                remaining(attemptDeadline)
                return StreamingData(sabr, if (offline) response else null)
            }
            val resolved = resolveStreams(streamingData, visitor, web)
            val streams = if (config.preferAvc) preferMultipleAvcQualities(resolved) else resolved
            remaining(attemptDeadline)
            if (!hasAdaptiveStreams(streams)) {
                Logger.info { "Spoof client $client has no resolved adaptive audio/video pair" }
                return null
            }
            Logger.debug { "Built stream response for $videoId using ${client.friendlyName}" }
            return StreamingData(streams, client, if (offline) response else null)
        }
    }

    private fun localeFor(client: ClientType): Locale =
        config.locale.takeUnless { client.multiAudioTracks } ?: Locale.getDefault()

    private fun send(
        client: ClientType,
        visitor: String?,
        web: WebPlayer.Session?,
        attestation: WebPlayer.Attestation?,
        attemptDeadline: Long,
    ): ByteArray {
        val json = innertubeBody(client, videoId, visitor, localeFor(client), web, attestation, reloadToken)
        if (client == ClientType.WEB) {
            val response = checkNotNull(web).player(json, checkNotNull(attestation).client)
            if (response.size > MAX_RESPONSE_BYTES)
                throw IOException("Player response is too large")
            return response
        }
        return http.send(client, json, attestation, attemptDeadline)
    }
}
