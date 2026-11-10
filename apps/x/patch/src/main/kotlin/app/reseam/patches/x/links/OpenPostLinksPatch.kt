// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.links

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.disableFeatureSwitches
import app.reseam.patches.x.featureswitches.featureSwitchOverrides
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val openPostLinks = patch(XPatchStrings.openPostLinks) {
    description(XPatchStrings.openPostLinksDescription)
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section(XSettingsPages.Links, XStrings.sectionLinks, XSettings.openPostLinks))

    execute {
        // The status link matcher hands the post to Home as a timeline injection while this switch is on.
        disableFeatureSwitches(XSettings.openPostLinks, "android_home_timeline_status_injections_deeplink_enabled")
    }
}
