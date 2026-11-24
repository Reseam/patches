// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.navigation

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

val phoneLayout = patch(XPatchStrings.phoneLayout) {
    description(XPatchStrings.phoneLayoutDescription)
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section(XSettingsPages.Navigation, XStrings.sectionNavigation, XSettings.phoneLayout))

    execute {
        // X moves the navigation bar to the side only when this is on and the window is at least 600 dp wide.
        disableFeatureSwitches(XSettings.phoneLayout, "android_large_screens_horizontal_inset_enabled")
    }
}
