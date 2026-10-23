// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SaveToDeviceMenu")

package app.reseam.youtubemusic.downloads

import android.content.Context
import android.view.View
import app.reseam.runtime.Export
import app.reseam.youtube.core.Proto
import app.reseam.youtube.core.bytes
import app.reseam.youtube.core.findActivity
import app.reseam.youtube.core.string
import app.reseam.youtube.savetodevice.isPlaylistSaved
import app.reseam.youtube.savetodevice.isVideoSaved
import app.reseam.youtube.savetodevice.savePlaylist
import app.reseam.youtube.savetodevice.saveVideo
import app.reseam.youtubesavetodevice.settings.SaveToDeviceStrings
import java.util.WeakHashMap

// A menu item's service renderer: text, icon and the command a tap runs.
private const val SERVICE_ITEM = 66441155
private const val ITEM_TEXT = 1
private const val ITEM_ICON = 2
private const val ITEM_COMMAND = 3
private const val TEXT_CONTENT = 4
private const val ICON_TYPE = 1
private const val ICON_OPEN_IN_PHONE = 987L

// The download item's command for a song and for a playlist.
private const val OFFLINE_VIDEO_ENDPOINT = 73080600
private const val OFFLINE_PLAYLIST_ENDPOINT = 73082583
private const val ENDPOINT_ID = 1

private sealed interface Saved {
    class Video(val videoId: String) : Saved

    class Playlist(val playlistId: String) : Saved
}

private val savedItems = WeakHashMap<Any, Saved>()

// R8 renames the menu item message, so the Save to device patch emits these bodies.
@Export fun itemBytes(item: Any): ByteArray = error("Replaced by the Save to device patch")

@Export fun parseItem(bytes: ByteArray): Any = error("Replaced by the Save to device patch")

/** Puts Save to device after the download item of a song or playlist that is downloaded. */
@Export
fun addItems(context: Context, items: MutableList<Any?>?) {
    if (items == null) return
    for (index in items.indices.reversed()) {
        val item = items[index] ?: continue
        val renderer = Proto.bytes(itemBytes(item), SERVICE_ITEM) ?: continue
        val command = Proto.bytes(renderer, ITEM_COMMAND) ?: continue
        val saved =
            Proto.bytes(command, OFFLINE_VIDEO_ENDPOINT)?.let { Proto.string(it, ENDPOINT_ID) }
                ?.takeIf { isVideoSaved(context, it) }
                ?.let(Saved::Video)
                ?: Proto.bytes(command, OFFLINE_PLAYLIST_ENDPOINT)?.let { Proto.string(it, ENDPOINT_ID) }
                    ?.takeIf { isPlaylistSaved(context, it) }
                    ?.let(Saved::Playlist)
                ?: continue
        val saveItem = parseItem(saveItemBytes(renderer))
        savedItems[saveItem] = saved
        items.add(index + 1, saveItem)
    }
}

/** Runs after the menu closed for a tapped item, and saves when the item was Save to device. */
@Export
fun onClick(item: Any?, view: View?) {
    val saved = item?.let(savedItems::get) ?: return
    val activity = view?.context?.findActivity() ?: return
    when (saved) {
        is Saved.Video -> saveVideo(activity, saved.videoId)
        is Saved.Playlist -> savePlaylist(activity, saved.playlistId)
    }
}

// The download item with this action's text and icon, and no command: a tap only closes the menu.
private fun saveItemBytes(renderer: ByteArray): ByteArray {
    val text = Proto.Writer().string(TEXT_CONTENT, SaveToDeviceStrings.saveToDevice).toByteArray()
    val icon = Proto.Writer().number(ICON_TYPE, ICON_OPEN_IN_PHONE).toByteArray()
    val item = Proto.replace(renderer, mapOf(ITEM_TEXT to text, ITEM_ICON to icon, ITEM_COMMAND to null))
    return Proto.Writer().bytes(SERVICE_ITEM, item).toByteArray()
}
