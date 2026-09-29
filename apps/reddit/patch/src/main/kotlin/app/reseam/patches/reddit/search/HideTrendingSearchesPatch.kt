// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.reddit.search

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.fieldOfType
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.after
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.core.FeedElements
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.RedditSettings
import app.reseam.patches.reddit.core.feedElementClassNames
import app.reseam.patches.reddit.core.redditSettings

val hideTrendingSearches = patch("Hide trending searches") {
    description("Removes the Trending section from the search screen.")
    compatibleWith(REDDIT)
    settings(redditSettings, section("Search", RedditSettings.hideTrendingSearches))

    execute {
        val trendingQuery = feedElementClassNames(listOf("SearchTrendingQuery(id="))
        // Reddit skips a search list with no children, header included.
        searchList.method("<init>").after(RedditSettings.hideTrendingSearches) {
            thisObject.set(searchListChildren, call(FeedElements.without, thisObject.field(searchListChildren), string(trendingQuery)))
        }
    }
}

private val searchList = klass("searchList") { strings("SearchTypeaheadList(id=") }

private val searchListChildren = searchList.fieldOfType(Type.List)
