// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("QueueContext")

package app.reseam.youtube.queue

import android.util.LruCache
import app.reseam.runtime.Export
import app.reseam.youtube.core.Endpoints
import app.reseam.youtube.core.Proto
import app.reseam.youtube.video.VideoInformation
import com.google.protobuf.MessageLite

private const val ENDPOINT_PLAYLIST = 2
private const val ENDPOINT_PLAYLIST_ITEM = 13
private const val CONTEXT_TYPE = 1
private const val CONTEXT_PLAYLIST = 5
private const val CONTEXT_VIDEO = 6
private const val CONTEXT_PLAYLIST_ITEM = 7
private const val CONTEXT_UPDATED = 12
private const val TYPE_QUEUE = 1L
private const val TYPE_SINGLE_VIDEO = 3L
private const val QUEUE_PLAYLIST_PREFIX = "QP"
private const val TRACKED_VIDEOS = 32

private data class QueueItem(val playlistId: String, val itemId: String)

private val queueItems = LruCache<String, QueueItem>(TRACKED_VIDEOS)

@Export
fun watched(currentVideo: MessageLite?) {
    if (currentVideo == null) return
    val endpoint = Proto.bytes(currentVideo.toByteArray(), Endpoints.WATCH) ?: return
    val videoId = Proto.string(endpoint, Endpoints.WATCH_VIDEO_ID) ?: return
    val playlistId = Proto.string(endpoint, ENDPOINT_PLAYLIST)
    val itemId = Proto.string(endpoint, ENDPOINT_PLAYLIST_ITEM)
    if (itemId != null && playlistId != null && playlistId.startsWith(QUEUE_PLAYLIST_PREFIX)) {
        queueItems.put(videoId, QueueItem(playlistId, itemId))
    } else {
        queueItems.remove(videoId)
    }
}

@Export
fun complete(current: ByteArray?): ByteArray? {
    val videoId = VideoInformation.videoId
    if (videoId.isEmpty()) return null
    val hasVideo = current?.let { Proto.bytes(it, CONTEXT_VIDEO) } != null
    if (hasVideo) return null
    val item = queueItems[videoId]
    if (item == null && current != null) return null
    return Proto.Writer()
        .apply {
            number(CONTEXT_TYPE, if (item == null) TYPE_SINGLE_VIDEO else TYPE_QUEUE)
            item?.let { string(CONTEXT_PLAYLIST, it.playlistId) }
            string(CONTEXT_VIDEO, videoId)
            item?.let { string(CONTEXT_PLAYLIST_ITEM, it.itemId) }
            current?.let { Proto.bytes(it, CONTEXT_UPDATED) }?.let { bytes(CONTEXT_UPDATED, it) }
        }
        .toByteArray()
}
