// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SaveToDeviceMenu")

package app.reseam.youtube.downloads

import android.content.Context
import android.view.View
import app.reseam.runtime.Export
import app.reseam.youtube.core.findActivity
import app.reseam.youtube.savetodevice.onMenuItemClick
import app.reseam.youtube.savetodevice.register
import app.reseam.youtube.savetodevice.saveMenuItem

// An icon YouTube draws in menus: a phone with a download arrow.
private const val ICON_OUTLINE_MOBILE_DOWNLOAD = 760L

// R8 renames the menu item message, so the Save to device patch emits these bodies.
@Export fun itemBytes(item: Any): ByteArray = error("Replaced by the Save to device patch")

@Export fun parseItem(bytes: ByteArray): Any = error("Replaced by the Save to device patch")

/** Save to device for the downloaded video a Downloads menu is built for, or null when it is not complete. */
@Export
fun saveItem(context: Context, downloadItem: Any): Any? {
    val save = saveMenuItem(context, itemBytes(downloadItem), ICON_OUTLINE_MOBILE_DOWNLOAD) ?: return null
    return parseItem(save.bytes).also { register(it, save) }
}

/** Runs after the menu closed for a tapped item; [anchor] is the view the menu opened from. */
@Export fun onClick(item: Any?, anchor: View?) = onMenuItemClick(item, anchor?.context?.findActivity())
