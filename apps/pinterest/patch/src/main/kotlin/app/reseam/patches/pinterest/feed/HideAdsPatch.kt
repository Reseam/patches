// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.pinterest.feed

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.pinterest.core.PINTEREST
import app.reseam.patches.pinterest.core.pinterestSettings
import app.reseam.patches.pinterest.refs.pinRefs
import app.reseam.pinterest.feed.bindings.PinFeeds
import app.reseam.pinterest.settings.FeedSettings
import app.reseam.pinterest.settings.PinterestStrings

val hideAds = patch("Hide ads") {
    description("Hides promoted pins.")
    compatibleWith(PINTEREST)
    dependsOn(pinRefs)
    settings(pinterestSettings, section(PinterestStrings.sectionFeed, FeedSettings.hideAds))

    execute {
        gate(FeedSettings.hideAds) {
            filterFeed(PinFeeds.removePromoted)
        }
    }
}
