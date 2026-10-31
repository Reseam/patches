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

internal fun PatchRuntime.hideFeedCards() {
    val crowdfundingBox = requireNotNull(resources.id("layout", "donation_companion")) {
        "layout/donation_companion is missing"
    }.toLong()
    val crowdfunding = method("crowdfundingBox") {
        flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
        literals(crowdfundingBox)
    }
    gate(FeedSettings.hideCrowdfundingBox) {
        crowdfunding.points("crowdfundingView") {
            invokeVirtual { name("inflate"); returns(Type.View) }
        }.single()
            .next { resultOf(Type.View) }
            .captureAs("view", Type.View)
            .after {
                LayoutViews.hideCrowdfundingBox(capture("view"))
            }
    }

    val albumCard = requireNotNull(resources.id("layout", "album_card")) {
        "layout/album_card is missing"
    }.toLong()
    val albumCards = method("albumCards") {
        flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
        literals(albumCard)
    }
    gate(FeedSettings.hideAlbumCards) {
        albumCards.points("albumCardView") {
            invokeVirtual { name("inflate"); returns(Type.View) }
            argument(1) { literal(albumCard) }
        }.single().next { resultOf(Type.View) }
            .captureAs("view", Type.View)
            .after {
                LayoutViews.hideAlbumCard(capture("view"))
            }
    }
}
