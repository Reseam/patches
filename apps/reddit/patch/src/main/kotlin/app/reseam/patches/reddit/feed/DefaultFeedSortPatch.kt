// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.reddit.feed

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.reddit.RedditPatchStrings
import app.reseam.patches.reddit.core.REDDIT
import app.reseam.patches.reddit.core.redditSettings
import app.reseam.reddit.settings.RedditSettings
import app.reseam.reddit.settings.RedditStrings
import app.reseam.reddit.sort.bindings.FeedSort

private const val SORT_TYPE = "Lcom/reddit/listing/model/sort/SortType;"
private const val SORT_TIME_FRAME = "Lcom/reddit/listing/model/sort/SortTimeFrame;"

val defaultFeedSort = patch(RedditPatchStrings.defaultFeedSort) {
    description(RedditPatchStrings.defaultFeedSortDescription)
    compatibleWith(REDDIT)
    settings(redditSettings, section(RedditStrings.sectionFeed, RedditSettings.defaultFeedSort, RedditSettings.defaultFeedTimeFrame))

    execute {
        // The sort passed in is the fallback for a feed without a saved sort; a saved one still wins.
        savedSort.before {
            val sortType = FeedSort.sortType(param(1))
            whenNotNull(sortType) {
                param(2).assign(newInstance(sort.descriptor, "($SORT_TYPE$SORT_TIME_FRAME)V", sortType, FeedSort.timeFrame(sortType)))
            }
        }
    }
}

private val sort = klass("sort") { strings("Sort(sortType=", ", sortTimeFrame=") }

// Reads the sort saved for a feed key and listing type, falling back to the sort passed in.
private val savedSort = method("savedSort") {
    strings("ListingSortUseCase:SortType:", "ListingSortUseCase:SortTimeFrame:")
    params(Type.String, "com.reddit.listing.common.ListingType", sort.descriptor)
}
