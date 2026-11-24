// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("AndroidPoToken")

package app.reseam.youtube.spoof

import android.os.SystemClock
import android.util.Base64
import app.reseam.bundle.Logger
import app.reseam.bundle.isMainThread
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.web.droidguardReady
import app.reseam.youtube.web.mintAndroidToken

private const val INTEGRITY_MODE = 1
private const val MINT_TIMEOUT_MILLISECONDS = 10_000L
private const val READY_TIMEOUT_MILLISECONDS = 3_000L
private const val READY_TTL_MILLISECONDS = 60_000L

/**
 * Whether the token server can supply native tokens. The client is chosen before the app asks for
 * one, so a server without DroidGuard sessions must be known up front for playback to fall through
 * to the next client.
 */
internal object NativeTokens {
    private var checkedAt: Long? = null
    private var ready = false

    @Synchronized
    fun available(deadline: Long): Boolean {
        val now = SystemClock.elapsedRealtime()
        checkedAt?.takeIf { now - it < READY_TTL_MILLISECONDS }?.let { return ready }
        ready =
            try {
                droidguardReady(minOf(deadline, now + READY_TIMEOUT_MILLISECONDS))
            } catch (exception: Exception) {
                Logger.info(exception) { "Token server readiness check failed" }
                false
            }
        checkedAt = now
        return ready
    }

    // A failed mint can be the network as much as the server, so it only forces a fresh check.
    @Synchronized
    fun recheck() {
        checkedAt = null
    }
}

/**
 * Supplies the integrity token the native Android client needs, minted by the Reseam token server
 * from a genuine Play Services session. The app keeps its own streams; only the token is replaced.
 */
@Export
fun mintNativePoToken(binding: ByteArray?, mode: Int): ByteArray? {
    if (mode != INTEGRITY_MODE || binding == null || !InstalledStreams.currentIsNative()) return null
    // The native client mints off the main thread; never block it if that ever changes.
    if (isMainThread()) return null
    return try {
        val token = mintAndroidToken(binding, SystemClock.elapsedRealtime() + MINT_TIMEOUT_MILLISECONDS)
        Logger.debug { "Supplied a server integrity token for native playback" }
        Base64.decode(token, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    } catch (exception: Exception) {
        Logger.error(exception) { "Could not mint a native integrity token" }
        NativeTokens.recheck()
        null
    }
}
