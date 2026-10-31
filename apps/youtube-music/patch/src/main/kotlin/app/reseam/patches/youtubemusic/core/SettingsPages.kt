// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.core

import app.reseam.patch.settings.SettingsPage
import app.reseam.patches.bundle.lucide
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

internal object YouTubeMusicSettingsPages {
    val Playback = SettingsPage("playback", YouTubeMusicStrings.pagePlayback, order = 0, summary = YouTubeMusicStrings.pagePlaybackSummary, icon = lucide("play"))
    val Player = SettingsPage("player", YouTubeMusicStrings.pagePlayer, order = 10, summary = YouTubeMusicStrings.pagePlayerSummary, icon = lucide("disc-3"))
    val Ads = SettingsPage("ads", YouTubeMusicStrings.pageAds, order = 20, summary = YouTubeMusicStrings.pageAdsSummary, icon = lucide("megaphone-off"))
    val Interface = SettingsPage("interface", YouTubeMusicStrings.pageInterface, order = 30, summary = YouTubeMusicStrings.pageInterfaceSummary, icon = lucide("layout-dashboard"))
    val Advanced = SettingsPage("advanced", YouTubeMusicStrings.pageAdvanced, order = 40, summary = YouTubeMusicStrings.pageAdvancedSummary, icon = lucide("sliders-horizontal"))

    val Menus = SettingsPage("interface.menus", YouTubeMusicStrings.pageInterfaceMenus, Interface, order = 0, summary = YouTubeMusicStrings.pageInterfaceMenusSummary, icon = lucide("menu"))
}
