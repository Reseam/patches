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
import app.reseam.youtube.web.mintAndroidToken

private const val INTEGRITY_MODE = 1
private const val MINT_TIMEOUT_MILLISECONDS = 10_000L

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
        null
    }
}
