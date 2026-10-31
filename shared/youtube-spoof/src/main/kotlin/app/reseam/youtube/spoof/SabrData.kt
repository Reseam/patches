// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import android.net.Uri
import app.reseam.youtube.core.Proto
import app.reseam.youtube.web.WebPlayer
import java.io.IOException

internal data class SabrData(
    val videoId: String,
    val client: ClientType,
    val streams: ByteArray,
    val nativeCommonConfig: Any,
    val uri: Uri,
    val attestation: WebPlayer.Attestation,
)

internal fun resolveSabrData(
    videoId: String,
    client: ClientType,
    response: PlayerResponse,
    web: WebPlayer.Session,
    attestation: WebPlayer.Attestation,
): SabrData {
    // StreamingData.serverAbrStreamingUrl (15); common.requestConfig (2).ustreamerConfig (4).
    val streamingData =
        checkNotNull(response.streamingData) { "Playable responses have streaming data" }
    val encodedUrl = Proto.bytes(streamingData, 15)
    val common = response.mediaCommonConfig
    val ustreamer = common?.let { Proto.bytes(it, 2) }?.let { Proto.bytes(it, 4) }
    if (encodedUrl == null || ustreamer == null || ustreamer.isEmpty()) {
        throw IOException("$client response has no complete SABR configuration")
    }
    val issuedUri = Uri.parse(encodedUrl.toString(Charsets.UTF_8))
    if (!isUsableStreamUrl(issuedUri)) throw IOException("Invalid SABR streaming URL")
    val mimeTypes =
        Proto.fields(streamingData)
            .filter { it.number == 3 && it.isFormat }
            .mapNotNull {
                val bytes = checkNotNull(it.bytes)
                Proto.bytes(bytes, 5)
                    ?.takeIf { Proto.number(bytes, 1, 0) > 0 }
                    ?.toString(Charsets.UTF_8)
            }
    if (!hasAudioAndVideo(mimeTypes))
        throw IOException("SABR response has no audio/video format pair")
    val uri =
        issuedUri.getQueryParameter("n")?.let { challenge ->
            val unlocked = web.solveN(setOf(challenge))
            Uri.parse(
                replaceStreamParameters(issuedUri, mapOf("n" to checkNotNull(unlocked[challenge])))
            )
        } ?: issuedUri
    val streams =
        Proto.replace(streamingData, mapOf(15 to uri.toString().toByteArray(Charsets.UTF_8)))
    return SabrData(
        videoId,
        client,
        streams,
        parseCommonConfig(checkNotNull(common)),
        uri,
        attestation,
    )
}
