// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.core

import app.reseam.patch.settings.toggle

object RedditSettings {
    val hideFeedAds by toggle("Hide feed ads", summary = "Removes promoted posts from feeds and search results.", default = true)
    val hideCommentAds by toggle("Hide comment ads", summary = "Stops loading ads between comments.", default = true)
    val hideRecommendedCommunities by toggle("Hide recommended communities", summary = "Removes the community suggestions shelf from feeds.", default = true)
    val hideTrendingSearches by toggle("Hide trending searches", summary = "Removes Trending Today queries from the search screen.", default = true)
    val disableScreenshotBanner by toggle("Disable screenshot banner", summary = "No share prompt after taking a screenshot.", default = true)
    val skipNsfwWarning by toggle("Skip NSFW community warning", summary = "Opens mature communities without the confirmation dialog.", default = false)
}
