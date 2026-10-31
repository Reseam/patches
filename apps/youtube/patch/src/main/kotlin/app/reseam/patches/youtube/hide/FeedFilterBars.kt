// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.youtube.hidelayout.bindings.LayoutViews
import app.reseam.youtube.settings.FeedSettings

internal fun PatchRuntime.hideFeedFilterBars() {
    val filterBarHeight = requireNotNull(resources.id("dimen", "filter_bar_height")) {
        "dimen/filter_bar_height is missing"
    }.toLong()
    val filterBar = method("filterBarHeight") {
        flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
        literals(filterBarHeight)
        hasParam("android.widget.LinearLayout")
    }
    gate(FeedSettings.hideFilterBarFeedInFeed) {
        filterBar.points("feedFilterBarHeight") {
            invokeVirtual { name("getDimensionPixelSize"); params(Type.Int); returns(Type.Int) }
            argument(1) { literal(filterBarHeight) }
        }.single()
            .next { resultOf(Type.Int) }
            .captureAs("height", Type.Int)
            .after {
                capture("height").assign(int(0))
            }
    }

    val barContainerHeight = requireNotNull(resources.id("dimen", "bar_container_height")) {
        "dimen/bar_container_height is missing"
    }.toLong()
    val searchChipBar = method("searchChipBar") {
        flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
        literals(barContainerHeight)
        hasParam("android.widget.LinearLayout")
    }
    gate(FeedSettings.hideFilterBarFeedInSearch) {
        searchChipBar.points("searchFilterBarHeight") {
            invokeVirtual { name("getDimensionPixelSize"); params(Type.Int); returns(Type.Int) }
            argument(1) { literal(barContainerHeight) }
        }.single()
            .next { resultOf(Type.Int) }
            .captureAs("height", Type.Int)
            .after {
                capture("height").assign(int(0))
            }
    }

    val relatedChipMargin = requireNotNull(resources.id("layout", "related_chip_cloud_reduced_margins")) {
        "layout/related_chip_cloud_reduced_margins is missing"
    }.toLong()
    val relatedChipCloud = method("relatedChipCloud") {
        flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
        literals(relatedChipMargin)
    }
    gate(FeedSettings.hideFilterBarFeedInRelatedVideos) {
        relatedChipCloud.points("relatedFilterBar") {
            invokeVirtual { name("inflate"); returns(Type.View) }
            argument(1) { literal(relatedChipMargin) }
        }.single()
            .next { resultOf(Type.View) }
            .captureAs("view", Type.View)
            .after {
                LayoutViews.hideInRelatedVideos(capture("view"))
            }
    }
}
