// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.settings.SettingsPage
import app.reseam.patch.settings.Vector
import app.reseam.youtube.settings.YouTubeStrings

/** YouTube's menu structure; the SDK only knows generic pages and sections. */
internal object YouTubeSettingsPages {
    val Video = SettingsPage("video", YouTubeStrings.pageVideo, order = 0, summary = YouTubeStrings.pageVideoSummary, icon = Vector.lucide("clapperboard"))
    val Player = SettingsPage("player", YouTubeStrings.pagePlayer, order = 10, summary = YouTubeStrings.pagePlayerSummary, icon = Vector.lucide("tv-minimal-play"))
    val Shorts = SettingsPage("shorts", YouTubeStrings.pageShorts, order = 20, summary = YouTubeStrings.pageShortsSummary, icon = Vector.lucide("film"))
    val Ads = SettingsPage("ads", YouTubeStrings.pageAds, order = 30, summary = YouTubeStrings.pageAdsSummary, icon = Vector.lucide("megaphone-off"))
    val Appearance = SettingsPage("appearance", YouTubeStrings.pageAppearance, order = 40, summary = YouTubeStrings.pageAppearanceSummary, icon = Vector.lucide("palette"))
    val Downloads = SettingsPage("downloads", YouTubeStrings.pageDownloads, order = 50, summary = YouTubeStrings.pageDownloadsSummary, icon = Vector.lucide("download"))
    val Advanced = SettingsPage("advanced", YouTubeStrings.pageAdvanced, order = 60, summary = YouTubeStrings.pageAdvancedSummary, icon = Vector.lucide("sliders-horizontal"))

    val Controls = SettingsPage("player.controls", YouTubeStrings.pagePlayerControls, Player, order = 0, summary = YouTubeStrings.pagePlayerControlsSummary, icon = Vector.lucide("hand"))
    val Overlay = SettingsPage("player.overlay", YouTubeStrings.pagePlayerOverlay, Player, order = 10, summary = YouTubeStrings.pagePlayerOverlaySummary, icon = Vector.lucide("layers"))
    val Feed = SettingsPage("appearance.feed", YouTubeStrings.pageAppearanceFeed, Appearance, order = 0, summary = YouTubeStrings.pageAppearanceFeedSummary, icon = Vector.lucide("layout-list"))
    val Comments = SettingsPage("appearance.comments", YouTubeStrings.pageAppearanceComments, Appearance, order = 10, summary = YouTubeStrings.pageAppearanceCommentsSummary, icon = Vector.lucide("message-square"))
    val Description = SettingsPage("appearance.description", YouTubeStrings.pageAppearanceDescription, Appearance, order = 20, summary = YouTubeStrings.pageAppearanceDescriptionSummary, icon = Vector.lucide("text-align-start"))
    val Channel = SettingsPage("appearance.channel", YouTubeStrings.pageAppearanceChannel, Appearance, order = 30, summary = YouTubeStrings.pageAppearanceChannelSummary, icon = Vector.lucide("user-round"))
    val Filters = SettingsPage("appearance.filters", YouTubeStrings.pageAppearanceFilters, Appearance, order = 40, summary = YouTubeStrings.pageAppearanceFiltersSummary, icon = Vector.lucide("funnel"))
}
