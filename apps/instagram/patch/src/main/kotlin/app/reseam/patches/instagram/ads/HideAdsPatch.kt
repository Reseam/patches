// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.ads

import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.alwaysReturn
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.instagram.settings.FeedSettings
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck
import app.reseam.patches.meta.sponsoredContentInjection

val hideAds = patch(InstagramPatchStrings.hideAds) {
    description(InstagramPatchStrings.hideAdsDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionFeed, FeedSettings.hideAds))

    execute {
        gate(FeedSettings.hideAds) {
            sponsoredContentInjection.alwaysReturn(false)
        }
    }
}

