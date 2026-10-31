// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import app.reseam.youtube.core.Proto
import app.reseam.youtube.core.bytes
import app.reseam.youtube.core.number
import app.reseam.youtube.core.string

internal data class PlayerResponse(
    val status: Int,
    val reason: String?,
    val streamingData: ByteArray?,
    val mediaCommonConfig: ByteArray?,
) {
    data class StreamUrl(val url: String?, val signatureCipher: String?)
}

internal const val PLAYER_STATUS_OK = 0
private const val PLAYABILITY_STATUS = 2
private const val STREAMING_DATA = 4
private const val PLAYER_CONFIG = 15
private const val MEDIA_COMMON_CONFIG = 215771584
private const val URL = 2
private const val SIGNATURE_CIPHER = 48

internal fun parsePlayerResponse(response: ByteArray): PlayerResponse {
    val fields = Proto.fields(response)
    // Repeated playability messages merge their fields, rather than replacing the whole status.
    val playability =
        fields
            .filter { it.number == PLAYABILITY_STATUS && it.wireType == Proto.WIRE_LENGTH }
            .flatMap { Proto.fields(checkNotNull(it.bytes)) }
    return PlayerResponse(
        playability.number(1)?.toInt() ?: PLAYER_STATUS_OK,
        playability.string(2),
        fields.bytes(STREAMING_DATA),
        fields.bytes(PLAYER_CONFIG)?.let { Proto.bytes(it, MEDIA_COMMON_CONFIG) },
    )
}

internal fun streamUrls(streamingData: ByteArray): List<PlayerResponse.StreamUrl> =
    Proto.fields(streamingData)
        .filter { it.isFormat }
        .mapNotNull { streamUrl(checkNotNull(it.bytes)) }

internal fun withResolvedUrls(
    streamingData: ByteArray,
    resolve: (PlayerResponse.StreamUrl) -> String?,
): ByteArray =
    Proto.rewriteLength(streamingData) { number, format ->
        if (number !in 2..3) format
        else
            streamUrl(format)?.let(resolve)?.let { resolved ->
                Proto.rewriteLength(format) { field, value ->
                    value.takeUnless { field == URL || field == SIGNATURE_CIPHER }
                } + Proto.Writer().string(URL, resolved).toByteArray()
            }
    }

internal fun streamUrl(format: ByteArray): PlayerResponse.StreamUrl? {
    val locations =
        Proto.fields(format)
            .filter {
                it.wireType == Proto.WIRE_LENGTH &&
                    (it.number == URL || it.number == SIGNATURE_CIPHER)
            }
            .filter { it.string().isNotEmpty() }
    return locations
        .firstOrNull { it.number == URL }
        ?.let { PlayerResponse.StreamUrl(it.string(), null) }
        ?: locations
            .lastOrNull { it.number == SIGNATURE_CIPHER }
            ?.let { PlayerResponse.StreamUrl(null, it.string()) }
}

internal val Proto.Field.isFormat: Boolean
    get() = number in 2..3 && wireType == Proto.WIRE_LENGTH

internal fun withStreamingData(streamingData: ByteArray): ByteArray =
    Proto.Writer().bytes(STREAMING_DATA, streamingData).toByteArray()
