// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.core

import app.reseam.patch.settings.SettingsPage
import app.reseam.patch.settings.Vector
import app.reseam.twitch.settings.TwitchStrings

internal object TwitchSettingsPages {
    val Video = SettingsPage("video", TwitchStrings.pageVideo, order = 0, summary = TwitchStrings.pageVideoSummary, icon = Vector.lucide("clapperboard"))
    val Player = SettingsPage("player", TwitchStrings.pagePlayer, order = 10, summary = TwitchStrings.pagePlayerSummary, icon = Vector.lucide("tv-minimal-play"))
    val Chat = SettingsPage("chat", TwitchStrings.pageChat, order = 20, summary = TwitchStrings.pageChatSummary, icon = Vector.lucide("message-square"))
    val Ads = SettingsPage("ads", TwitchStrings.pageAds, order = 30, summary = TwitchStrings.pageAdsSummary, icon = Vector.lucide("megaphone-off"))
    val Appearance = SettingsPage("appearance", TwitchStrings.pageAppearance, order = 40, summary = TwitchStrings.pageAppearanceSummary, icon = Vector.lucide("palette"))
    val Advanced = SettingsPage("advanced", TwitchStrings.pageAdvanced, order = 60, summary = TwitchStrings.pageAdvancedSummary, icon = Vector.lucide("sliders-horizontal"))
}
