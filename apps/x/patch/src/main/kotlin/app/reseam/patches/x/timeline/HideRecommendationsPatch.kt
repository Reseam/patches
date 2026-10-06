// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.appEntry
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.TimelineQueryFilter
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettings
import app.reseam.patches.x.core.xSettings

val hideRecommendations = patch("Hide recommendations") {
    description("Removes the Who to follow, Subscribe to, and Communities to join modules from timelines.")
    compatibleWith(X)
    dependsOn(timelineQueryFilter)
    settings(xSettings, section("Timeline", XSettings.hideRecommendations))

    execute {
        appEntry {
            call(TimelineQueryFilter.hideRecommendations, string(XSettings.hideRecommendations.key), bool(XSettings.hideRecommendations.default))
        }
    }
}
