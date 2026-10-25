// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("QueueReply")

package app.reseam.youtube.queue

import android.net.Uri
import android.util.Base64
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.Endpoints
import app.reseam.youtube.core.Proto
import app.reseam.youtube.core.debug
import com.google.common.util.concurrent.ListenableFuture
import com.google.protobuf.MessageLite
import java.util.concurrent.TimeUnit

private const val REQUEST_QUEUE = 3
private const val REQUEST_OPERATION = 1
private const val REQUEST_TARGET = 2
private const val OPERATION_PLAY_NEXT = 2L
private const val RESPONSE_COMMAND = 4
internal const val PLAYER_STATE_COMMAND = 469942096
private const val QUEUE_INSERT = 465666947
private const val WATCH_MINIMIZED = 339694216
private const val QUEUE_TOKEN = 71
internal const val POSITION_NEXT = 1L
private const val POSITION_LAST = 2L
private const val QUEUE_WATCH_PARAMS = "yAQB"

@Export
fun wrap(response: ListenableFuture<Any?>, params: String?): ListenableFuture<Any?> =
    queueCommand(params)?.let { QueueResponse(response, it) } ?: response

@Export fun parse(response: ByteArray): Any? = null

private fun queueCommand(params: String?): ByteArray? {
    if (params.isNullOrEmpty()) return null
    try {
        val queue =
            Proto.bytes(Base64.decode(Uri.decode(params), Base64.URL_SAFE), REQUEST_QUEUE)
                ?: return null
        val target =
            requireNotNull(Proto.bytes(queue, REQUEST_TARGET)) { "Queue request has no target" }
        val targetVideo = requireNotNull(Proto.bytes(target, 1)) { "Queue target has no video" }
        val videoId =
            requireNotNull(Proto.string(targetVideo, 1)) { "Queue target has no video ID" }
        val position =
            if (Proto.number(queue, REQUEST_OPERATION, 0) == OPERATION_PLAY_NEXT) POSITION_NEXT
            else POSITION_LAST
        return Proto.Writer()
            .bytes(PLAYER_STATE_COMMAND, playerStateCommand(videoId, position))
            .toByteArray()
    } catch (exception: IllegalArgumentException) {
        Logger.debug { "Active-devices parameters are not a queue request" }
        return null
    }
}

internal fun playerStateCommand(videoId: String, position: Long): ByteArray {
    val insertion = Proto.Writer().string(1, token(videoId, position))
    val watch = Proto.Writer().string(Endpoints.WATCH_VIDEO_ID, videoId).string(5, QUEUE_WATCH_PARAMS)
    val minimized = Proto.Writer().message(2, Proto.Writer().message(Endpoints.WATCH, watch))
    return Proto.Writer()
        .message(1, Proto.Writer().message(QUEUE_INSERT, insertion))
        .message(2, Proto.Writer().message(WATCH_MINIMIZED, minimized))
        .toByteArray()
}

/** The video a player state command shaped like [playerStateCommand]'s plays minimized. */
internal fun playerStateVideoId(command: ByteArray): String? =
    Proto.bytes(command, 2)
        ?.let { Proto.bytes(it, WATCH_MINIMIZED) }
        ?.let { Proto.bytes(it, 2) }
        ?.let { Proto.bytes(it, Endpoints.WATCH) }
        ?.let { Proto.string(it, Endpoints.WATCH_VIDEO_ID) }

private fun token(videoId: String, position: Long): String {
    val token =
        Proto.Writer()
            .message(
                QUEUE_TOKEN,
                Proto.Writer().message(1, Proto.Writer().string(1, videoId).number(3, position)),
            )
            .toByteArray()
    return Uri.encode(Base64.encodeToString(token, Base64.URL_SAFE or Base64.NO_WRAP))
}

private class QueueResponse(
    private val response: ListenableFuture<Any?>,
    private val command: ByteArray,
) : ListenableFuture<Any?> by response {
    // Null replacements are retried on the next get, as in the app's response wrapper.
    @Volatile private var replaced: Any? = null

    override fun get(): Any? = replace(response.get())

    override fun get(timeout: Long, unit: TimeUnit): Any? = replace(response.get(timeout, unit))

    private fun replace(message: Any?): Any? {
        val cached = replaced
        if (cached != null) return cached
        val bytes = (message as MessageLite).toByteArray()
        val current = Proto.bytes(bytes, RESPONSE_COMMAND)
        val result =
            if (current != null && Proto.bytes(current, Endpoints.BROWSE) != null) {
                parse(Proto.replace(bytes, mapOf(RESPONSE_COMMAND to command)))
            } else {
                message
            }
        replaced = result
        return result
    }
}
