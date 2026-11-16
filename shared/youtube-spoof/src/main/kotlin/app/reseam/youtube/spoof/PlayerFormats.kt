// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import android.view.Display
import app.reseam.youtube.core.Proto
import app.reseam.youtube.core.VideoCodec
import app.reseam.youtube.core.string

internal fun hasAdaptiveStreams(streamingData: ByteArray): Boolean {
    val mimeTypes =
        Proto.fields(streamingData)
            .filter { it.number == 3 && it.isFormat }
            .mapNotNull {
                val bytes = checkNotNull(it.bytes)
                format(bytes).mimeType.takeUnless { streamUrl(bytes)?.url.isNullOrEmpty() }
            }
    return hasAudioAndVideo(mimeTypes)
}

internal fun hasAudioAndVideo(mimeTypes: List<String>): Boolean =
    listOf("audio/", "video/").all { prefix -> mimeTypes.any { it.startsWith(prefix) } }

// A lone VP9 quality hides the longer AVC ladder in YouTube's codec-family picker.
internal fun preferMultipleAvcQualities(streamingData: ByteArray): ByteArray {
    val fields = Proto.fields(streamingData)
    val qualities =
        fields
            .filter { it.number == 3 && it.isFormat }
            .map { format(checkNotNull(it.bytes)) }
            .filter { !it.qualityLabel.isNullOrEmpty() }
            .groupBy { it.codec }
            .mapValues { (_, formats) -> formats.map { it.qualityLabel }.toSet().size }
    if (qualities[Codec.VP9] != 1 || (qualities[Codec.AVC] ?: 0) <= 1) return streamingData
    return Proto.rewriteLength(streamingData) { number, bytes ->
        bytes.takeUnless { number == 3 && "vp9" in format(it).mimeType }
    }
}

private const val MIME_TYPE = 5
private const val COLOR_INFO = 33
private const val TRANSFER_CHARACTERISTICS = 2
private val hdrTypesByTransfer =
    mapOf(
        16L to Display.HdrCapabilities.HDR_TYPE_HDR10,
        18L to Display.HdrCapabilities.HDR_TYPE_HLG,
    )

// The app's own player request reports the codecs and HDR types it plays; other clients get every format.
internal fun PlayerResponse.withoutUnplayableFormats(hdrTypes: IntArray): PlayerResponse =
    copy(
        streamingData =
            streamingData?.let { data ->
                Proto.rewriteLength(data) { number, format ->
                    format.takeUnless { number in 2..3 && !playable(format, hdrTypes) }
                }
            }
    )

private fun playable(format: ByteArray, hdrTypes: IntArray): Boolean {
    if (videoCodec(Proto.string(format, MIME_TYPE).orEmpty())?.enabled == false) return false
    val transfer = Proto.bytes(format, COLOR_INFO)?.let { Proto.number(it, TRANSFER_CHARACTERISTICS, 0) }
    return hdrTypesByTransfer[transfer]?.let { it in hdrTypes } ?: true
}

private fun videoCodec(mimeType: String): VideoCodec? =
    when {
        "av01" in mimeType -> VideoCodec.AV1
        "vp9" in mimeType || "vp09" in mimeType -> VideoCodec.VP9
        else -> null
    }

private enum class Codec {
    AVC,
    VP9,
    OTHER,
}

private data class Format(val mimeType: String, val qualityLabel: String?) {
    val codec: Codec
        get() =
            when {
                "avc1" in mimeType -> Codec.AVC
                "vp9" in mimeType -> Codec.VP9
                else -> Codec.OTHER
            }
}

private fun format(bytes: ByteArray): Format =
    Proto.fields(bytes).let { Format(it.string(MIME_TYPE).orEmpty(), it.string(26)) }
