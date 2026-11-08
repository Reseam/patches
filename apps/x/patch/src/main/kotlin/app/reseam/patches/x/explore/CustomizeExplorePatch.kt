// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.explore

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.appEntry
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.timeline.timelineQueryFilter
import app.reseam.x.explore.bindings.ExploreTabs
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import app.reseam.x.timeline.bindings.TimelineQueryFilter

val customizeExplore = patch("Customize Explore") {
    description("Hides Grok stories, trends and Explore tabs such as News and Sports.")
    compatibleWith(X)
    dependsOn(timelineQueryFilter)
    settings(
        xSettings,
        section(
            XSettingsPages.Explore,
            XStrings.sectionExplore,
            XSettings.hideGrokStories,
            XSettings.hideExploreTrends,
            XSettings.hideTrendingTab,
            XSettings.hideNewsTab,
            XSettings.hideSportsTab,
            XSettings.hideEntertainmentTab,
        ),
    )

    execute {
        appEntry {
            TimelineQueryFilter.hideStories(string(XSettings.hideGrokStories.key), bool(XSettings.hideGrokStories.default))
            TimelineQueryFilter.hideExploreTrends(string(XSettings.hideExploreTrends.key), bool(XSettings.hideExploreTrends.default))
        }
        ExploreTabs.segmentedTimelineId.implement {
            returnValue(param(0).cast(urpTimeline.descriptor).field(segmentedTimelineId))
        }
        // Every tabbed URP page, Explore included, gets its tab list from the page fragment here.
        urpTimelines.after { returnValue(ExploreTabs.visible(result)) }
    }
}

private val urpTimeline = klass("urpTimeline") { strings("UrpTimeline(title=") }

// The field toString prints just before scribeConfig.
private val segmentedTimelineId = urpTimeline.method("toString")
    .point { string(", scribeConfig=") }
    .previous { opcode(Opcode.IGET_OBJECT) }
    .field("segmentedTimelineId")

private val urpPageFragment = klass("urpPageFragment") { strings("UrpPageFragment(page_body=") }

private val urpTimelines = method("urpTimelines") {
    returns(Type.List)
    params(urpPageFragment.descriptor)
}
