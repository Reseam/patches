// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.feed

import app.reseam.patch.patch
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.FeedSettings
import app.reseam.tiktok.settings.TikTokStrings

private val RULES: Map<ToggleSetting, String> =
    mapOf(
        FeedSettings.hideLive to "LIVE",
        FeedSettings.hidePhotoPosts to "PHOTOS",
        FeedSettings.hideShopPosts to "SHOP",
        FeedSettings.hideAiGenerated to "AI_GENERATED",
        FeedSettings.hidePaidPartnerships to "PAID_PARTNERSHIPS",
        FeedSettings.hideSeries to "SERIES",
        FeedSettings.hideSubscriberOnly to "SUBSCRIBER_ONLY",
        FeedSettings.hideStories to "STORIES",
        FeedSettings.hideSuggestedAccounts to "CARDS",
        FeedSettings.hideVerifiedAccounts to "VERIFIED",
    )

val hideFeedContent =
    patch("Hide feed content") {
        description(
            "Removes chosen kinds of posts, such as LIVE, Shop or AI-generated posts, from TikTok's feeds and search."
        )
        compatibleWith(TIKTOK)
        dependsOn(feedFilter)
        settings(
            tiktokSettings,
            section(TikTokSettingsPages.Feed, TikTokStrings.sectionFeed, *RULES.keys.toTypedArray()),
        )

        execute {
            RULES.forEach { (toggle, rule) -> hidePosts(toggle, rule) }
        }
    }
