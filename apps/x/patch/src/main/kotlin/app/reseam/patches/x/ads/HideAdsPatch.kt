// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.ads

import app.reseam.patch.appEntry
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides
import app.reseam.patches.x.timeline.timelineQueryFilter
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import app.reseam.x.timeline.bindings.TimelineQueryFilter

val hideAds = patch("Hide ads") {
    description("Removes promoted posts and Google ads from timelines.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides, timelineQueryFilter)
    settings(xSettings, section(XSettingsPages.Ads, XStrings.sectionAds, XSettings.hideAds))

    execute {
        disableFeatureSwitches(XSettings.hideAds, "android_x_lite_ssp_ads_enabled", "ssp_ads_home_enabled")

        appEntry {
            TimelineQueryFilter.hidePromoted(string(XSettings.hideAds.key), bool(XSettings.hideAds.default))
        }
    }
}
