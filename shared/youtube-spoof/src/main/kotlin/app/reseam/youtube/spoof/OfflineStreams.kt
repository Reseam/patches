// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("OfflineStreams")

package app.reseam.youtube.spoof

import android.util.LruCache
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.Proto
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val STREAMING_DATA = 4
private const val VIDEO_DETAILS = 11
private const val OFFLINE_STATE = 14
private const val VIDEO_ID = 1
private const val ADAPTIVE_FORMATS = 3
private const val FORMAT_MIME_TYPE = 5
private const val FORMAT_OFFLINE_ELIGIBLE = 58
private const val OPUS_AUDIO = "audio/webm"
private const val STATE_TOKEN = 1
private const val STATE_VALIDITY = 3
private const val STATE_ACTION = 5
private const val ACTION_UNKNOWN = 0L
private const val ACTION_OK = 1L
private const val ACTION_DISABLE = 3L
private val validitySeconds = 30.days.inWholeSeconds
private val retryMilliseconds = 30.seconds.inWholeMilliseconds
private val expiryMarginMilliseconds = 30.minutes.inWholeMilliseconds
private const val REMEMBERED_VIDEOS = 32

private data class Answer(val response: ByteArray?, val validUntil: Long)

private val answers = LruCache<String, Answer>(REMEMBERED_VIDEOS)

@Export
fun replaceRefused(playerResponse: ByteArray): ByteArray? {
    val details = Proto.bytes(playerResponse, VIDEO_DETAILS)
    val videoId = details?.let { Proto.string(it, VIDEO_ID) }
    val refusal = Proto.bytes(playerResponse, OFFLINE_STATE)
    if (videoId == null || refusal == null) return null
    val answer =
        answers.get(videoId)?.takeIf { it.validUntil > System.currentTimeMillis() }
            ?: answer(videoId, refusal).also {
                synchronized(answers) { answers.put(videoId, it) }
            }
    return answer.response
}

/**
 * The offline state allowed again when the server disables a download or withholds its action, as it
 * does for free accounts; null for allowed states and the server's other actions, such as delete.
 */
@Export
fun allowRefusedState(state: ByteArray): ByteArray? =
    when (Proto.number(state, STATE_ACTION, ACTION_UNKNOWN)) {
        ACTION_UNKNOWN, ACTION_DISABLE -> allowed(state)
        else -> null
    }

private fun answer(videoId: String, refusal: ByteArray): Answer {
    val streams = fetchOfflineStreams(videoId)
    if (streams == null || streams.playerResponse == null || streams.sabr != null) {
        Logger.info { "No direct offline streams for $videoId" }
        return Answer(null, System.currentTimeMillis() + retryMilliseconds)
    }
    Logger.info { "Offline streams for $videoId from ${streams.client.friendlyName}" }
    val response =
        Proto.replace(
            streams.playerResponse,
            mapOf(
                STREAMING_DATA to
                    offlineEligible(checkNotNull(Proto.bytes(streams.response, STREAMING_DATA))),
                OFFLINE_STATE to allowed(refusal),
            ),
        )
    return Answer(response, streams.expiresAt - expiryMarginMilliseconds)
}

private fun allowed(refusal: ByteArray): ByteArray =
    Proto.Writer()
        .apply {
            Proto.bytes(refusal, STATE_TOKEN)?.let { bytes(STATE_TOKEN, it) }
        }
        .number(STATE_VALIDITY, validitySeconds)
        .number(STATE_ACTION, ACTION_OK)
        .toByteArray()

private fun offlineEligible(streamingData: ByteArray): ByteArray =
    Proto.rewriteLength(streamingData) { number, format ->
        val mime = if (number == ADAPTIVE_FORMATS) Proto.string(format, FORMAT_MIME_TYPE) else null
        if (mime == null || mime.startsWith(OPUS_AUDIO)) format
        else format + Proto.Writer().number(FORMAT_OFFLINE_ELIGIBLE, 1).toByteArray()
    }
