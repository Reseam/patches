// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.appEntry
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import app.reseam.x.timeline.bindings.TimelineQueryFilter

val hideRecommendations = patch("Hide recommendations") {
    description("Removes the Who to follow, Subscribe to, and Communities to join modules from timelines.")
    compatibleWith(X)
    dependsOn(timelineQueryFilter)
    settings(xSettings, section(XStrings.sectionTimeline, XSettings.hideRecommendations))

    execute {
        appEntry {
            TimelineQueryFilter.hideRecommendations(string(XSettings.hideRecommendations.key), bool(XSettings.hideRecommendations.default))
        }
    }
}
