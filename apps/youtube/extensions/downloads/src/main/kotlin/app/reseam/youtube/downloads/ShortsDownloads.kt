// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ShortsDownloads")

package app.reseam.youtube.downloads

import app.reseam.runtime.Export
import app.reseam.youtube.core.Proto
import app.reseam.youtube.settings.DownloadSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.video.VideoInformation
import java.lang.ref.WeakReference

// A menu's items, and the parts of one that make it the app's download item for a video.
private const val MENU_ITEMS = 1
private const val SERVICE_ITEM = 66441155
private const val ITEM_TEXT = 1
private const val ITEM_COMMAND = 3
private const val ITEM_LOADING = 16
private const val TEXT_RUNS = 1
private const val RUN_TEXT = 1
private const val OFFLINE_VIDEO_ENDPOINT = 73080600
private const val ENDPOINT_VIDEO_ID = 1
private const val ENDPOINT_ACTION = 2
private const val ENDPOINT_OFFLINEABILITY = 4
// The action the server's download items carry; the app resolves it from the video's download state.
private const val ACTION_FROM_STATE = 11L

// PlayerResponse.playabilityStatus.offlineability: whether and in which qualities the video can be downloaded.
private const val PLAYABILITY_STATUS = 2
private const val OFFLINEABILITY = 10

// Items of the Shorts player's own menu are elements under this path.
private val SHORTS_PLAYER_MENU = "/youtube/app/shorts_overflow_menu/".toByteArray()
private const val RECENT_RESPONSES = 8

private class Response(val videoId: String, val message: WeakReference<Any>)

private val responses = ArrayDeque<Response>()

// R8 renames the protobuf messages, so the Download Shorts patch emits these bodies.
@Export fun messageBytes(message: Any): ByteArray = error("Replaced by the Download Shorts patch")

@Export fun parseMenu(bytes: ByteArray): Any = error("Replaced by the Download Shorts patch")

@Export
fun onPlayerResponse(videoId: String?, response: Any?) {
    if (videoId == null || response == null) return
    synchronized(responses) {
        responses.removeAll { it.videoId == videoId }
        responses.addLast(Response(videoId, WeakReference(response)))
        while (responses.size > RECENT_RESPONSES) responses.removeFirst()
    }
}

/**
 * The Shorts player's menu with the app's download item for the Short, built from the Short's own offlineability.
 * The app then shows it as Download, or as its downloaded actions, the way it does in other menus.
 */
@Export
fun withDownloadItem(menu: Any?): Any? {
    if (menu == null || !VideoInformation.lastVideoIdIsShort) return menu
    val bytes = messageBytes(menu)
    val items = Proto.fields(bytes).filter { it.number == MENU_ITEMS }.mapNotNull { it.bytes }
    if (items.none { it.contains(SHORTS_PLAYER_MENU) } || items.any(::isDownloadItem)) return menu
    val videoId = VideoInformation.videoId
    val offlineability = if (DownloadSettings.unlockDownloads.value) unlockedOfflineability() else serverOfflineability(videoId)
    if (offlineability == null) return menu
    return parseMenu(bytes + Proto.Writer().bytes(MENU_ITEMS, downloadItem(videoId, offlineability)).toByteArray())
}

private fun serverOfflineability(videoId: String): ByteArray? =
    synchronized(responses) { responses.lastOrNull { it.videoId == videoId }?.message?.get() }
        ?.let { Proto.bytes(messageBytes(it), PLAYABILITY_STATUS) }
        ?.let { Proto.bytes(it, OFFLINEABILITY) }

private fun isDownloadItem(item: ByteArray): Boolean =
    Proto.bytes(item, SERVICE_ITEM)
        ?.let { Proto.bytes(it, ITEM_COMMAND) }
        ?.let { Proto.bytes(it, OFFLINE_VIDEO_ENDPOINT) } != null

// Marked as loading, the item is resolved by the app into the action that fits the video's download state.
private fun downloadItem(videoId: String, offlineability: ByteArray): ByteArray {
    val endpoint =
        Proto.Writer()
            .string(ENDPOINT_VIDEO_ID, videoId)
            .number(ENDPOINT_ACTION, ACTION_FROM_STATE)
            .bytes(ENDPOINT_OFFLINEABILITY, offlineability)
    val text = Proto.Writer().message(TEXT_RUNS, Proto.Writer().string(RUN_TEXT, YouTubeStrings.downloadShortsChecking))
    val service =
        Proto.Writer()
            .message(ITEM_TEXT, text)
            .message(ITEM_COMMAND, Proto.Writer().message(OFFLINE_VIDEO_ENDPOINT, endpoint))
            .number(ITEM_LOADING, 1)
    return Proto.Writer().message(SERVICE_ITEM, service).toByteArray()
}

private fun ByteArray.contains(part: ByteArray): Boolean =
    (0..size - part.size).any { start -> part.indices.all { this[start + it] == part[it] } }
