// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.search

import app.reseam.patch.Type
import app.reseam.patch.fieldOfType
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.RedditPatchStrings
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.feedElementClassNames
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.reddit.feed.bindings.FeedElements
import app.reseam.reddit.settings.RedditSettings
import app.reseam.reddit.settings.RedditStrings

val hideTrendingSearches = patch(RedditPatchStrings.hideTrendingSearches) {
    description(RedditPatchStrings.hideTrendingSearchesDescription)
    compatibleWith(REDDIT)
    settings(redditSettings, section(RedditStrings.sectionSearch, RedditSettings.hideTrendingSearches))

    execute {
        val trendingQuery = feedElementClassNames(listOf("SearchTrendingQuery(id="))
        // Reddit skips a search list with no children, header included.
        gate(RedditSettings.hideTrendingSearches) {
            searchList.method("<init>").after {
                thisObject.set(searchListChildren, FeedElements.without(thisObject.field(searchListChildren), string(trendingQuery)))
            }
        }
    }
}

private val searchList = klass("searchList") { strings("SearchTypeaheadList(id=") }

private val searchListChildren = searchList.fieldOfType(Type.List)
