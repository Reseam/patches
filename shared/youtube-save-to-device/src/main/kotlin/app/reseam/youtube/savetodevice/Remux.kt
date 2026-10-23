// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.savetodevice

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

internal enum class Container(val muxerFormat: Int, val extension: String) {
    MP4(MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4, "mp4"),
    WEBM(MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM, "webm"),
}

internal class UnsupportedFormatException(mimeTypes: List<String>) : IOException("No container for $mimeTypes")

private const val DEFAULT_SAMPLE_BUFFER = 2 * 1024 * 1024

private val MP4_TYPES =
    setOf(MediaFormat.MIMETYPE_AUDIO_AAC, MediaFormat.MIMETYPE_VIDEO_AVC, MediaFormat.MIMETYPE_VIDEO_HEVC) +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) setOf(MediaFormat.MIMETYPE_VIDEO_AV1)
        else emptySet()
private val WEBM_TYPES =
    setOf(
        MediaFormat.MIMETYPE_AUDIO_OPUS,
        MediaFormat.MIMETYPE_AUDIO_VORBIS,
        MediaFormat.MIMETYPE_VIDEO_VP8,
        MediaFormat.MIMETYPE_VIDEO_VP9,
    )

private class Track(val extractor: MediaExtractor, val format: MediaFormat) {
    val mimeType: String = checkNotNull(format.getString(MediaFormat.KEY_MIME))
    var index = -1
}

/** The streams' samples as one file in the container their codecs allow, without re-encoding. */
internal fun remux(streams: List<CachedStream>, output: File, onProgress: (Float) -> Unit): Container {
    val tracks =
        streams.map { stream ->
            val extractor = MediaExtractor().apply { setDataSource(stream) }
            if (extractor.trackCount != 1) throw IOException("A stream has ${extractor.trackCount} tracks")
            extractor.selectTrack(0)
            Track(extractor, extractor.getTrackFormat(0))
        }
    try {
        val mimeTypes = tracks.map(Track::mimeType)
        val container =
            when {
                mimeTypes.all(MP4_TYPES::contains) -> Container.MP4
                mimeTypes.all(WEBM_TYPES::contains) -> Container.WEBM
                else -> throw UnsupportedFormatException(mimeTypes)
            }
        val muxer = MediaMuxer(output.path, container.muxerFormat)
        try {
            tracks.forEach { it.index = muxer.addTrack(it.format) }
            tracks.firstNotNullOfOrNull { it.format.integer(MediaFormat.KEY_ROTATION) }?.let(muxer::setOrientationHint)
            muxer.start()
            copySamples(tracks, muxer, onProgress)
            muxer.stop()
        } finally {
            muxer.release()
        }
        return container
    } finally {
        tracks.forEach { it.extractor.release() }
    }
}

// Writes samples in presentation order across tracks, so the muxer interleaves them as playback reads them.
private fun copySamples(tracks: List<Track>, muxer: MediaMuxer, onProgress: (Float) -> Unit) {
    val size = tracks.maxOf { it.format.integer(MediaFormat.KEY_MAX_INPUT_SIZE) ?: DEFAULT_SAMPLE_BUFFER }
    val buffer = ByteBuffer.allocateDirect(size)
    val info = MediaCodec.BufferInfo()
    val duration = tracks.maxOf { it.format.long(MediaFormat.KEY_DURATION) ?: 0L }
    var reported = -1
    while (true) {
        // Times can start below zero (encoder delay), so the end of a track is its missing sample.
        val track = tracks.filter { it.extractor.sampleTrackIndex >= 0 }.minByOrNull { it.extractor.sampleTime } ?: break
        val extractor = track.extractor
        val length = extractor.readSampleData(buffer, 0)
        val keyFrame = extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
        info.set(0, length, extractor.sampleTime, if (keyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
        muxer.writeSampleData(track.index, buffer, info)
        if (duration > 0) {
            val percent = (info.presentationTimeUs * 100 / duration).toInt()
            if (percent != reported) {
                reported = percent
                onProgress(percent / 100f)
            }
        }
        extractor.advance()
    }
}

private fun MediaFormat.integer(key: String): Int? = if (containsKey(key)) getInteger(key) else null

private fun MediaFormat.long(key: String): Long? = if (containsKey(key)) getLong(key) else null
