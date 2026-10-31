// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.core

import app.reseam.patch.settings.SettingsPage
import app.reseam.patches.bundle.lucide
import app.reseam.tiktok.settings.TikTokStrings

internal object TikTokSettingsPages {
    val Feed =
        SettingsPage(
            "feed",
            TikTokStrings.pageFeed,
            order = 0,
            summary = TikTokStrings.pageFeedSummary,
            icon = lucide("layout-list"),
        )
    val Video =
        SettingsPage(
            "video",
            TikTokStrings.pageVideo,
            order = 10,
            summary = TikTokStrings.pageVideoSummary,
            icon = lucide("clapperboard"),
        )
    val Playback =
        SettingsPage(
            "playback",
            TikTokStrings.pagePlayback,
            order = 20,
            summary = TikTokStrings.pagePlaybackSummary,
            icon = lucide("play"),
        )
    val SearchAndApp =
        SettingsPage(
            "search_and_app",
            TikTokStrings.pageSearchAndApp,
            order = 30,
            summary = TikTokStrings.pageSearchAndAppSummary,
            icon = lucide("layout-dashboard"),
        )
    val Inbox =
        SettingsPage(
            "inbox",
            TikTokStrings.pageInbox,
            order = 40,
            summary = TikTokStrings.pageInboxSummary,
            icon = lucide("inbox"),
        )
    val Downloads =
        SettingsPage(
            "downloads",
            TikTokStrings.pageDownloads,
            order = 50,
            summary = TikTokStrings.pageDownloadsSummary,
            icon = lucide("download"),
        )
    val Privacy =
        SettingsPage(
            "privacy",
            TikTokStrings.pagePrivacy,
            order = 60,
            summary = TikTokStrings.pagePrivacySummary,
            icon = lucide("shield"),
        )
    val Advanced =
        SettingsPage(
            "advanced",
            TikTokStrings.pageAdvanced,
            order = 70,
            summary = TikTokStrings.pageAdvancedSummary,
            icon = lucide("sliders-horizontal"),
        )
}
