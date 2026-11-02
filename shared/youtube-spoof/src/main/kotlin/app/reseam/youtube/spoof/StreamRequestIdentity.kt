// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import android.net.Uri
import android.os.SystemClock
import app.reseam.bundle.Logger
import app.reseam.youtube.core.Proto
import app.reseam.youtube.core.debug
import java.util.TreeMap
import java.util.concurrent.TimeoutException

internal fun forwardedHeaders(playerHeaders: Map<String?, String?>?): Map<String, String> {
    val headers = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
    playerHeaders?.forEach { (key, value) ->
        if (
            key != null &&
                value != null &&
                forwardedHeaderNames.any { it.equals(key, ignoreCase = true) }
        ) {
            headers[key] = value
        }
    }
    // '+' is base64 data; only percent escapes are decoded.
    headers[StreamingDataRequest.VISITOR_ID_HEADER]?.let {
        headers[StreamingDataRequest.VISITOR_ID_HEADER] = Uri.decode(it)
    }
    return headers
}

private val forwardedHeaderNames =
    listOf(StreamingDataRequest.VISITOR_ID_HEADER, "X-GOOG-API-FORMAT-VERSION")

// YouTube 21.40 loads a video through get_watch, which bundles the player request (field 2) with the next request.
internal fun playerRequest(path: String, body: ByteArray?): ByteArray? =
    if (path.endsWith("/get_watch")) {
        try {
            body?.let { Proto.bytes(it, 2) }
        } catch (_: IllegalArgumentException) {
            null
        }
    } else body

internal fun playerRequestVideoId(request: ByteArray?): String? {
    if (request == null || request.isEmpty()) return null
    return try {
        Proto.string(request, 2)?.takeIf { it.isNotEmpty() }
    } catch (_: IllegalArgumentException) {
        null
    }
}

internal fun isOfflinePlayerRequest(request: ByteArray?): Boolean {
    if (request == null || request.isEmpty()) return false
    return try {
        Proto.number(request, 8, 0) != 0L
    } catch (_: IllegalArgumentException) {
        false
    }
}

internal fun reloadToken(request: ByteArray?): String? {
    if (request == null || request.isEmpty()) return null
    try {
        // PlayerRequest.playbackContext.reloadPlaybackContext.reloadPlaybackParams.token.
        val value =
            intArrayOf(4, 7, 1, 1).fold(request) { message, field ->
                Proto.bytes(message, field) ?: return null
            }
        return value.toString(Charsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        Logger.debug { "Native player request is not a protobuf reload request" }
        return null
    }
}

internal fun remaining(deadline: Long): Int {
    if (Thread.currentThread().isInterrupted) throw InterruptedException("Stream request cancelled")
    val remaining = deadline - SystemClock.elapsedRealtime()
    if (remaining <= 0) throw TimeoutException("Stream request deadline exceeded")
    return minOf(remaining, Int.MAX_VALUE.toLong()).toInt()
}
