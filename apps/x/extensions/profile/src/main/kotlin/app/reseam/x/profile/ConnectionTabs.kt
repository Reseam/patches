// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ConnectionTabs")

package app.reseam.x.profile

import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings

private const val SEGMENT = "segmentedTimelineId="
private val extraTabs = setOf("verified_followers", "followers_you_follow", "subscribers", "subscriptions")

// A tab is a data class whose toString names its segment; the field itself has no stable position.
private fun segment(timeline: Any): String = timeline.toString().substringAfter(SEGMENT, "").substringBefore(',')

@Export
fun visible(timelines: List<Any?>): List<Any?> =
    if (!XSettings.hideExtraFollowerTabs.value) timelines
    else timelines.filterNot { it != null && segment(it) in extraTabs }
