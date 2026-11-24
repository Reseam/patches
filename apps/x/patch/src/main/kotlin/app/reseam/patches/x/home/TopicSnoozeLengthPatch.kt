// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.home

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.patches.x.featureswitches.featureSwitchOverrides
import app.reseam.patches.x.featureswitches.forceFeatureSwitch
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val topicSnoozeLength = patch(XPatchStrings.topicSnoozeLength) {
    description(XPatchStrings.topicSnoozeLengthDescription)
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section(XSettingsPages.Home, XStrings.sectionHome, XSettings.topicSnoozeLength))

    execute {
        // For You's Snooze topics stores the snoozed topics until now plus this many minutes.
        forceFeatureSwitch("co_timeline_reset_period_minutes", XSettings.topicSnoozeLength)
    }
}
