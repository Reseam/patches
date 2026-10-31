// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ToolbarButtons")

package app.reseam.youtubemusic.layout

import android.view.MenuItem
import app.reseam.bundle.identifier
import app.reseam.runtime.Export
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtubemusic.settings.YouTubeMusicSettings

private val hiddenItems by lazy {
    val context = YouTubeContext.applicationContext
    mapOf(
        context.identifier("action_search", "id") to YouTubeMusicSettings.hideSearchButton,
        context.identifier("history_menu_item", "id") to YouTubeMusicSettings.hideHistoryButton,
    )
}

@Export
fun setVisible(item: MenuItem, visible: Boolean): MenuItem? {
    val hide = hiddenItems[item.itemId]
    return item.setVisible(visible && hide?.value != true)
}
