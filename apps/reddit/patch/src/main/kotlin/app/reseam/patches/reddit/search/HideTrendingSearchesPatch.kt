// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.search

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.skipWhen
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.redditSettings

val hideTrendingSearches = patch("Hide trending searches") {
    description("Removes Trending Today queries from the search screen.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Search", RedditSettings.hideTrendingSearches))

    execute {
        trendingQueryItem.skipWhen(RedditSettings.hideTrendingSearches)
    }
}

private val trendingQueryItem = method("trendingQueryItem") {
    strings("search_trending_item")
    returns(Type.Void)
}
