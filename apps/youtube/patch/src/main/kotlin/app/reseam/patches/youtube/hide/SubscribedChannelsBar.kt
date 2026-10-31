// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.youtube.hidelayout.bindings.LayoutViews
import app.reseam.youtube.settings.FeedSettings

internal fun PatchRuntime.hideSubscribedChannelsBar() {
    val parentContainer = requireNotNull(resources.id("id", "parent_container")) {
        "id/parent_container is missing"
    }.toLong()
    val subscribedChannels = method("subscribedChannelsBarConstructor") {
        flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
        literals(parentContainer)
        custom { classDef.instanceFields.any { it.fieldType == "Landroid/support/v7/widget/RecyclerView;" } }
    }
    gate(FeedSettings.hideSubscribedChannelsBar) {
        subscribedChannels.points("subscribedChannelsBar") {
            invokeVirtual { name("findViewById"); params(Type.Int); returns(Type.View) }
            argument(1) { literal(parentContainer) }
        }.single().next { resultOf(Type.View) }
            .captureAs("bar", Type.View)
            .after {
                LayoutViews.hideSubscribedChannelsBar(capture("bar"))
            }
    }

    val parentViewWidth = requireNotNull(resources.id("dimen", "parent_view_width_in_wide_mode")) {
        "dimen/parent_view_width_in_wide_mode is missing"
    }.toLong()
    val subscribedChannelsClass = klass(subscribedChannels.owner)
    val subscribedChannelsLandscape = method("subscribedChannelsLandscape") {
        inClass(subscribedChannelsClass)
        params()
        returns(Type.Void)
        literals(parentViewWidth)
    }
    gate(FeedSettings.hideSubscribedChannelsBar) {
        subscribedChannelsLandscape.points("subscribedChannelsLandscapeHeight") {
            invokeVirtual { name("getDimensionPixelSize"); params(Type.Int); returns(Type.Int) }
            argument(1) { literal(parentViewWidth) }
        }.single()
            .next { resultOf(Type.Int) }
            .captureAs("height", Type.Int)
            .after {
                capture("height").assign(int(0))
            }
    }
}
