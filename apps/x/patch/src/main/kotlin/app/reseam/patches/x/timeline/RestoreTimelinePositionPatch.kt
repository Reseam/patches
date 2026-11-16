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

private const val ONE_YEAR_MINUTES = "525600"

val restoreTimelinePosition = patch(XPatchStrings.restoreTimelinePosition) {
    description(XPatchStrings.restoreTimelinePositionDescription)
    compatibleWith(X)
    dependsOn(featureSwitchOverrides)
    settings(xSettings, section(XSettingsPages.Home, XStrings.sectionTimeline, XSettings.restoreTimelinePosition))

    execute {
        forceFeatureSwitches(
            XSettings.restoreTimelinePosition,
            "x_android_timeline_resume_position_enabled" to "true",
            "x_android_timeline_resume_position_max_age_minutes" to ONE_YEAR_MINUTES,
        )
    }
}
