// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import android.view.Display
import app.reseam.youtube.core.Proto
import app.reseam.youtube.core.VideoCodec
import app.reseam.youtube.core.bytes
import app.reseam.youtube.core.defaultDisplayHdrTypes
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
        bytes.takeUnless { number == 3 && format(it).codec == Codec.VP9 }
    }
}

private const val MIME_TYPE = 5
private const val QUALITY_LABEL = 26
private const val COLOR_INFO = 33
private const val TRANSFER_CHARACTERISTICS = 2
private val hdrTypesByTransfer =
    mapOf(16L to Display.HdrCapabilities.HDR_TYPE_HDR10, 18L to Display.HdrCapabilities.HDR_TYPE_HLG)

// The app's own player request reports the codecs and HDR types it plays; other clients return every format.
internal fun PlayerResponse.withoutUnplayableFormats(): PlayerResponse {
    val hdrTypes = defaultDisplayHdrTypes()
    return copy(
        streamingData =
            streamingData?.let { data ->
                Proto.rewriteLength(data) { number, bytes ->
                    bytes.takeUnless { number in 2..3 && !format(it).playable(hdrTypes) }
                }
            }
    )
}

private enum class Codec(val video: VideoCodec?) {
    AVC(null),
    VP9(VideoCodec.VP9),
    AV1(VideoCodec.AV1),
    OTHER(null),
}

private data class Format(val mimeType: String, val qualityLabel: String?, val transfer: Long?) {
    val codec: Codec
        get() =
            when {
                "avc1" in mimeType -> Codec.AVC
                "vp9" in mimeType || "vp09" in mimeType -> Codec.VP9
                "av01" in mimeType -> Codec.AV1
                else -> Codec.OTHER
            }

    fun playable(hdrTypes: IntArray): Boolean =
        codec.video?.enabled != false && hdrTypesByTransfer[transfer]?.let { it in hdrTypes } != false
}

private fun format(bytes: ByteArray): Format =
    Proto.fields(bytes).let { fields ->
        Format(
            fields.string(MIME_TYPE).orEmpty(),
            fields.string(QUALITY_LABEL),
            fields.bytes(COLOR_INFO)?.let { Proto.number(it, TRANSFER_CHARACTERISTICS, 0) },
        )
    }
