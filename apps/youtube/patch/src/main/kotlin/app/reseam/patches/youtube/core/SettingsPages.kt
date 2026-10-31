// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.settings.SettingsPage
import app.reseam.patches.bundle.lucide
import app.reseam.youtube.settings.YouTubeStrings

/** YouTube's menu structure; the SDK only knows generic pages and sections. */
internal object YouTubeSettingsPages {
    val Video = SettingsPage("video", YouTubeStrings.pageVideo, order = 0, summary = YouTubeStrings.pageVideoSummary, icon = lucide("clapperboard"))
    val Player = SettingsPage("player", YouTubeStrings.pagePlayer, order = 10, summary = YouTubeStrings.pagePlayerSummary, icon = lucide("tv-minimal-play"))
    val Shorts = SettingsPage("shorts", YouTubeStrings.pageShorts, order = 20, summary = YouTubeStrings.pageShortsSummary, icon = lucide("film"))
    val Ads = SettingsPage("ads", YouTubeStrings.pageAds, order = 30, summary = YouTubeStrings.pageAdsSummary, icon = lucide("megaphone-off"))
    val Appearance = SettingsPage("appearance", YouTubeStrings.pageAppearance, order = 40, summary = YouTubeStrings.pageAppearanceSummary, icon = lucide("palette"))
    val Downloads = SettingsPage("downloads", YouTubeStrings.pageDownloads, order = 50, summary = YouTubeStrings.pageDownloadsSummary, icon = lucide("download"))
    val Advanced = SettingsPage("advanced", YouTubeStrings.pageAdvanced, order = 60, summary = YouTubeStrings.pageAdvancedSummary, icon = lucide("sliders-horizontal"))

    val Controls = SettingsPage("player.controls", YouTubeStrings.pagePlayerControls, Player, order = 0, summary = YouTubeStrings.pagePlayerControlsSummary, icon = lucide("hand"))
    val Overlay = SettingsPage("player.overlay", YouTubeStrings.pagePlayerOverlay, Player, order = 10, summary = YouTubeStrings.pagePlayerOverlaySummary, icon = lucide("layers"))
    val Feed = SettingsPage("appearance.feed", YouTubeStrings.pageAppearanceFeed, Appearance, order = 0, summary = YouTubeStrings.pageAppearanceFeedSummary, icon = lucide("layout-list"))
    val Comments = SettingsPage("appearance.comments", YouTubeStrings.pageAppearanceComments, Appearance, order = 10, summary = YouTubeStrings.pageAppearanceCommentsSummary, icon = lucide("message-square"))
    val Description = SettingsPage("appearance.description", YouTubeStrings.pageAppearanceDescription, Appearance, order = 20, summary = YouTubeStrings.pageAppearanceDescriptionSummary, icon = lucide("text"))
    val Channel = SettingsPage("appearance.channel", YouTubeStrings.pageAppearanceChannel, Appearance, order = 30, summary = YouTubeStrings.pageAppearanceChannelSummary, icon = lucide("user-round"))
    val Filters = SettingsPage("appearance.filters", YouTubeStrings.pageAppearanceFilters, Appearance, order = 40, summary = YouTubeStrings.pageAppearanceFiltersSummary, icon = lucide("funnel"))
}
