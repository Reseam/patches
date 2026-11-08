// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ExploreTabs")

package app.reseam.x.explore

import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings

private val hideToggles =
    mapOf(
        "trending" to XSettings.hideTrendingTab,
        "news" to XSettings.hideNewsTab,
        "sports" to XSettings.hideSportsTab,
        "entertainment" to XSettings.hideEntertainmentTab,
    )

@Export external fun segmentedTimelineId(tab: Any): String?

// Explore's tab ids are bare (trending, news); other URP pages namespace theirs (followers:screen_name:…).
@Export
fun visible(tabs: List<Any>): List<Any> = tabs.filterNot { hideToggles[segmentedTimelineId(it)]?.value == true }
