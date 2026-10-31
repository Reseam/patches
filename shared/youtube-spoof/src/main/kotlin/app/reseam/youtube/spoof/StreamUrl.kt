// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import android.net.Uri
import app.reseam.bundle.Logger
import app.reseam.youtube.core.debug
import app.reseam.youtube.web.WebPlayer
import java.io.IOException

internal data class StreamSignature(val value: String, val parameter: String)

internal data class StreamUrl(val uri: Uri, val signature: StreamSignature?)

internal fun parseStreamUrl(location: PlayerResponse.StreamUrl): StreamUrl {
    location.url?.let {
        return StreamUrl(Uri.parse(it), null)
    }
    val cipher = Uri.parse("https://www.youtube.com/?${location.signatureCipher}")
    val url = cipher.getQueryParameter("url")
    val signature = cipher.getQueryParameter("s")
    val parameter = cipher.getQueryParameter("sp")
    if (url == null || signature.isNullOrEmpty()) {
        throw IllegalArgumentException("Incomplete stream signature cipher")
    }
    return StreamUrl(Uri.parse(url), StreamSignature(signature, parameter ?: "signature"))
}

internal fun isUsableStreamUrl(uri: Uri): Boolean =
    when {
        uri.scheme != "https" -> false
        uri.host?.endsWith(".googlevideo.com") != true -> false
        uri.path != "/videoplayback" -> false
        else -> streamExpiresAt(uri) > System.currentTimeMillis() + 60_000
    }

internal fun streamExpiresAt(uri: Uri): Long =
    uri.getQueryParameter("expire")
        ?.toLongOrNull()
        ?.takeIf { it in 0..Long.MAX_VALUE / 1000 }
        ?.times(1000) ?: 0

// Preserve unrelated query bytes and duplicate parameters: signatures can cover their encoding.
internal fun replaceStreamParameters(uri: Uri, replacements: Map<String, String>): String {
    val query = buildList {
        uri.encodedQuery
            ?.takeIf { it.isNotEmpty() }
            ?.split('&')
            ?.forEach { pair ->
                val separator = pair.indexOf('=')
                val name = Uri.decode(if (separator < 0) pair else pair.substring(0, separator))
                if (name !in replacements) add(pair)
            }
        replacements.forEach { (name, value) -> add("${Uri.encode(name)}=${Uri.encode(value)}") }
    }
    return uri.buildUpon().encodedQuery(query.joinToString("&")).build().toString()
}

internal fun resolveStreams(
    streamingData: ByteArray,
    visitor: String?,
    web: WebPlayer.Session?,
): ByteArray {
    val streams =
        streamUrls(streamingData)
            .mapNotNull { location ->
                try {
                    parseStreamUrl(location)
                        .takeIf {
                            isUsableStreamUrl(it.uri) && (web != null || it.signature == null)
                        }
                        ?.let { location to it }
                } catch (_: IllegalArgumentException) {
                    Logger.debug { "Ignoring malformed stream URL" }
                    null
                }
            }
            .toMap()
    if (streams.isEmpty()) throw IOException("Player response has no usable stream URLs")
    if (web == null) return withResolvedUrls(streamingData) { streams[it]?.uri?.toString() }
    val n = streams.values.mapNotNull { it.uri.getQueryParameter("n") }.toCollection(HashSet())
    val sig = streams.values.mapNotNull { it.signature?.value }.toCollection(HashSet())
    val unlocked = web.unlock(visitor, n, sig)
    return withResolvedUrls(streamingData) { location ->
        streams[location]?.let { stream ->
            replaceStreamParameters(
                stream.uri,
                buildMap {
                    stream.uri.getQueryParameter("n")?.let { put("n", solution(unlocked.n, it)) }
                    stream.signature?.let { put(it.parameter, solution(unlocked.sig, it.value)) }
                    put("pot", unlocked.poToken)
                },
            )
        }
    }
}

private fun solution(solutions: Map<String, String>, challenge: String): String =
    solutions[challenge]?.takeIf { it.isNotEmpty() }
        ?: throw IllegalArgumentException("Missing player challenge solution")
