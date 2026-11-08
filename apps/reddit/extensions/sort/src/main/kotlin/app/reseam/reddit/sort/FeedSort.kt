// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FeedSort")

package app.reseam.reddit.sort

import app.reseam.reddit.settings.RedditSettings
import app.reseam.runtime.Export
import com.reddit.listing.model.sort.SortTimeFrame
import com.reddit.listing.model.sort.SortType
import java.util.Locale

// Home always sorts by Best, and profile, saved and history listings keep their own sorts.
private val feeds = setOf("SUBREDDIT", "MULTIREDDIT", "POPULAR", "ALL")

@Export
fun sortType(listing: Enum<*>): SortType? {
    val sort = RedditSettings.defaultFeedSort.value
    if (sort == "off" || listing.name !in feeds) return null
    return SortType.valueOf(sort.uppercase(Locale.ROOT))
}

@Export
fun timeFrame(sort: SortType): SortTimeFrame? =
    if (sort == SortType.TOP || sort == SortType.CONTROVERSIAL) {
        SortTimeFrame.valueOf(RedditSettings.defaultFeedTimeFrame.value.uppercase(Locale.ROOT))
    } else {
        null
    }
