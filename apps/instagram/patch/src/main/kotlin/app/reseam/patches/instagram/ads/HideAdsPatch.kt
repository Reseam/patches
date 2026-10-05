// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.ads

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.FeedSettings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val hideAds = patch("Hide ads") {
    description("Stops Instagram from inserting sponsored posts into feed, stories, explore, reels and search.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Feed", FeedSettings.hideAds))

    execute {
        gate(FeedSettings.hideAds) {
            adInsertion.alwaysReturn(false)
        }
    }
}

private val adInsertion = method("adInsertion") {
    strings("Is ad pod", "cross_surface_duplicate_ad")
    returns(Type.Boolean)
}
