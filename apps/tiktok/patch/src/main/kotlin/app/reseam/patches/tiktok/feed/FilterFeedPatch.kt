// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.tiktok.feed

import app.reseam.patch.patch
import app.reseam.patch.settings.TextSetting
import app.reseam.patch.settings.section
import app.reseam.patches.tiktok.core.TIKTOK
import app.reseam.patches.tiktok.core.TikTokSettingsPages
import app.reseam.patches.tiktok.core.tiktokSettings
import app.reseam.tiktok.settings.FeedFilterSettings
import app.reseam.tiktok.settings.TikTokStrings

private val LIMITS: Map<TextSetting, String> =
    mapOf(
        FeedFilterSettings.hiddenAuthors to "AUTHORS",
        FeedFilterSettings.hiddenHashtags to "HASHTAGS",
        FeedFilterSettings.hiddenKeywords to "KEYWORDS",
        FeedFilterSettings.onlyRegions to "ONLY_REGIONS",
        FeedFilterSettings.hiddenRegions to "HIDDEN_REGIONS",
        FeedFilterSettings.minimumViews to "MINIMUM_VIEWS",
        FeedFilterSettings.minimumLikes to "MINIMUM_LIKES",
        FeedFilterSettings.maximumAgeDays to "MAXIMUM_AGE_DAYS",
        FeedFilterSettings.maximumLengthSeconds to "MAXIMUM_LENGTH_SECONDS",
    )

val filterFeed =
    patch("Filter feed") {
        description(
            "Hides posts by account, hashtag, caption words, region, views, likes, age or length in TikTok's feeds and search."
        )
        compatibleWith(TIKTOK)
        dependsOn(feedFilter)
        settings(
            tiktokSettings,
            section(
                TikTokSettingsPages.Feed,
                TikTokStrings.sectionFeedFilters,
                *LIMITS.keys.toTypedArray(),
            ),
        )

        execute {
            LIMITS.forEach { (setting, limit) -> limitPosts(setting, limit) }
        }
    }
