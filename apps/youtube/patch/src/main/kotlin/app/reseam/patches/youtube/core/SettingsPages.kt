// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.settings.SettingsPage

/** YouTube's menu structure; the SDK only knows generic pages and sections. */
internal object YouTubeSettingsPages {
    val Video = SettingsPage("video", "Video", order = 0)
    val Player = SettingsPage("player", "Player", order = 10)
    val Shorts = SettingsPage("shorts", "Shorts", order = 20)
    val Ads = SettingsPage("ads", "Ads", order = 30)
    val Appearance = SettingsPage("appearance", "Appearance", order = 40)
    val Downloads = SettingsPage("downloads", "Downloads", order = 50)
    val Advanced = SettingsPage("advanced", "Advanced", order = 60)

    val Controls = SettingsPage("player.controls", "Controls and gestures", Player, order = 0)
    val Overlay = SettingsPage("player.overlay", "Overlay and menus", Player, order = 10)
    val Feed = SettingsPage("appearance.feed", "Feed", Appearance, order = 0)
    val Comments = SettingsPage("appearance.comments", "Comments", Appearance, order = 10)
    val Description = SettingsPage("appearance.description", "Video description", Appearance, order = 20)
    val Channel = SettingsPage("appearance.channel", "Channel pages", Appearance, order = 30)
    val Filters = SettingsPage("appearance.filters", "Content filters", Appearance, order = 40)
}
