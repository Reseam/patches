// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.featureSwitchOverrides
import app.reseam.patches.x.featureswitches.forceFeatureSwitches
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val disableAutoRefresh = patch(XPatchStrings.disableAutoRefresh) {
    description(XPatchStrings.disableAutoRefreshDescription)
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section(XSettingsPages.Home, XStrings.sectionTimeline, XSettings.disableAutoRefresh))

    execute {
        forceFeatureSwitches(
            XSettings.disableAutoRefresh,
            "timeline_auto_refresh_on_foreground_timeout_millis" to Int.MAX_VALUE.toString(),
        )
    }
}
