// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.savetodevice

import android.app.Activity
import android.content.Context
import app.reseam.youtube.core.Proto
import app.reseam.youtubesavetodevice.settings.SaveToDeviceStrings
import java.util.WeakHashMap

// A menu item's service renderer: text, icon, the command a tap runs, and whether it is greyed out.
private const val SERVICE_ITEM = 66441155
private const val ITEM_TEXT = 1
private const val ITEM_ICON = 2
private const val ITEM_COMMAND = 3
private const val ITEM_DISABLED = 16
private const val TEXT_RUNS = 1
private const val RUN_TEXT = 1
private const val ICON_TYPE = 1

// The download item's command for a video and for a playlist.
private const val OFFLINE_VIDEO_ENDPOINT = 73080600
private const val OFFLINE_PLAYLIST_ENDPOINT = 73082583
private const val ENDPOINT_ID = 1

/** A Save to device menu item, as the app's menu item message bytes, for one download. */
class SaveMenuItem internal constructor(val bytes: ByteArray, internal val target: SaveTarget)

private val items = WeakHashMap<Any, SaveTarget>()

/**
 * Save to device for the download [downloadItem] is about, shaped like it with [iconType], or null when that
 * video or playlist is not fully downloaded.
 */
fun saveMenuItem(context: Context, downloadItem: ByteArray, iconType: Long): SaveMenuItem? {
    val renderer = Proto.bytes(downloadItem, SERVICE_ITEM) ?: return null
    val command = Proto.bytes(renderer, ITEM_COMMAND) ?: return null
    val store = OfflineStore(context)
    val target =
        Proto.bytes(command, OFFLINE_VIDEO_ENDPOINT)?.let { Proto.string(it, ENDPOINT_ID) }
            ?.takeIf(store::isSaved)
            ?.let(SaveTarget::Video)
            ?: Proto.bytes(command, OFFLINE_PLAYLIST_ENDPOINT)?.let { Proto.string(it, ENDPOINT_ID) }
                ?.takeIf(store::isPlaylistSaved)
                ?.let(SaveTarget::Playlist)
            ?: return null
    val run = Proto.Writer().string(RUN_TEXT, SaveToDeviceStrings.saveToDevice)
    val text = Proto.Writer().message(TEXT_RUNS, run).toByteArray()
    val icon = Proto.Writer().number(ICON_TYPE, iconType).toByteArray()
    // Without a command a tap only closes the menu, so a menu the app rebuilds from saved state does nothing.
    // The download item can be the placeholder shown while the download's state loads, which is greyed out.
    val item =
        Proto.replace(renderer, mapOf(ITEM_TEXT to text, ITEM_ICON to icon, ITEM_COMMAND to null, ITEM_DISABLED to null))
    return SaveMenuItem(Proto.Writer().bytes(SERVICE_ITEM, item).toByteArray(), target)
}

/** Remembers the app's message built from [item], so a tap on it can be recognized. */
fun register(message: Any, item: SaveMenuItem) {
    items[message] = item.target
}

/** Saves when [message] is a Save to device item; called after the menu closed for a tapped item. */
fun onMenuItemClick(message: Any?, activity: Activity?) {
    val target = message?.let(items::get) ?: return
    if (activity != null) request(activity, target)
}
