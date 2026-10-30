// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SaveToDeviceMenu")

package app.reseam.youtube.downloads

import app.reseam.runtime.Export
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.savetodevice.onMenuItemClick
import app.reseam.youtube.savetodevice.register
import app.reseam.youtube.savetodevice.saveMenuItem

// An icon YouTube draws in menus: a phone with a download arrow.
private const val ICON_OUTLINE_MOBILE_DOWNLOAD = 760L

// R8 renames the menu item message, so the Save to device patch emits these bodies.
@Export external fun itemBytes(item: Any): ByteArray

@Export external fun parseItem(bytes: ByteArray): Any

/** Save to device for the downloaded video a menu's Delete from downloads is for, or null when it is not complete. */
@Export
fun saveItem(deleteItem: Any): Any? {
    val save = saveMenuItem(YouTubeContext.applicationContext, itemBytes(deleteItem), ICON_OUTLINE_MOBILE_DOWNLOAD) ?: return null
    return parseItem(save.bytes).also { register(it, save) }
}

/** Runs after a menu closed for a tapped item. */
@Export fun onClick(item: Any?) = onMenuItemClick(item, YouTubeContext.mainActivity)
