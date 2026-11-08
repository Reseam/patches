// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.links

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val openPostLinks = patch("Open post links") {
    description("Opens a post link on the post itself instead of on top of the For You timeline.")
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section(XStrings.sectionLinks, XSettings.openPostLinks))

    execute {
        // The status link matcher hands the post to Home as a timeline injection while this switch is on.
        disableFeatureSwitches(XSettings.openPostLinks, "android_home_timeline_status_injections_deeplink_enabled")
    }
}
