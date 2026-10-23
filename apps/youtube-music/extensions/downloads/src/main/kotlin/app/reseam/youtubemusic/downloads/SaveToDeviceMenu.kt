// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SaveToDeviceMenu")

package app.reseam.youtubemusic.downloads

import android.content.Context
import android.view.View
import app.reseam.runtime.Export
import app.reseam.youtube.core.findActivity
import app.reseam.youtube.savetodevice.onMenuItemClick
import app.reseam.youtube.savetodevice.register
import app.reseam.youtube.savetodevice.saveMenuItem

// An icon YouTube Music draws in menus: a phone with an arrow into it.
private const val ICON_OPEN_IN_PHONE = 987L

// R8 renames the menu item message, so the Save to device patch emits these bodies.
@Export fun itemBytes(item: Any): ByteArray = error("Replaced by the Save to device patch")

@Export fun parseItem(bytes: ByteArray): Any = error("Replaced by the Save to device patch")

@Export
fun addItems(context: Context, items: MutableList<Any?>?) {
    if (items == null) return
    for (index in items.indices.reversed()) {
        val item = items[index] ?: continue
        val save = saveMenuItem(context, itemBytes(item), ICON_OPEN_IN_PHONE) ?: continue
        val message = parseItem(save.bytes)
        register(message, save)
        items.add(index + 1, message)
    }
}

@Export fun onClick(item: Any?, view: View?) = onMenuItemClick(item, view?.context?.findActivity())
