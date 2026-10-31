// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("MenuItems")

package app.reseam.youtubemusic.layout

import android.view.View
import android.view.ViewGroup
import app.reseam.runtime.Export
import app.reseam.youtubemusic.settings.YouTubeMusicSettings as Settings

private val hiddenIcons =
    mapOf(
        "MIX" to Settings.hideMenuStartMix,
        "QUEUE_MUSIC" to Settings.hideMenuAddToQueue,
        "BOOKMARK" to Settings.hideMenuLibrary,
        "BOOKMARK_BORDER" to Settings.hideMenuLibrary,
        "LIBRARY_ADD" to Settings.hideMenuLibrary,
        "LIBRARY_REMOVE" to Settings.hideMenuLibrary,
        "ALBUM" to Settings.hideMenuGoToAlbum,
        "ARTIST" to Settings.hideMenuGoToArtist,
        "PEOPLE_GROUP" to Settings.hideMenuSongCredits,
        "PIN_OUTLINE" to Settings.hideMenuSpeedDial,
        "PIN_OFF_OUTLINE" to Settings.hideMenuSpeedDial,
        "KEEP" to Settings.hideMenuSpeedDial,
        "KEEP_OFF" to Settings.hideMenuSpeedDial,
        "HIDE" to Settings.hideMenuNotInterested,
        "PERSON_CIRCLE_SLASH" to Settings.hideMenuDontRecommendArtist,
        "DISMISS_QUEUE" to Settings.hideMenuDismissQueue,
        "FLAG" to Settings.hideMenuReport,
        "SETTINGS_MATERIAL" to Settings.hideMenuQuality,
        "CAPTIONS" to Settings.hideMenuCaptions,
        "MOON_Z" to Settings.hideMenuSleepTimer,
    )

@Export
fun bind(row: View, icon: Enum<*>) {
    val hide = hiddenIcons[icon.name]?.value == true
    row.visibility = if (hide) View.GONE else View.VISIBLE
    // The menu list reserves space for a gone row unless its height is zero; rows are reused.
    val params = row.layoutParams ?: return
    val height = if (hide) 0 else ViewGroup.LayoutParams.WRAP_CONTENT
    if (params.height != height) {
        params.height = height
        row.layoutParams = params
    }
}
