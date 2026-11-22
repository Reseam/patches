// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.appEntry
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import app.reseam.x.timeline.bindings.TimelineQueryFilter

val hideDiscoverMore = patch(XPatchStrings.hideDiscoverMore) {
    description(XPatchStrings.hideDiscoverMoreDescription)
    compatibleWith(X)
    dependsOn(timelineQueryFilter)
    settings(xSettings, section(XSettingsPages.Home, XStrings.sectionTimeline, XSettings.hideDiscoverMore))

    execute {
        appEntry {
            TimelineQueryFilter.hideDiscoverMore(string(XSettings.hideDiscoverMore.key), bool(XSettings.hideDiscoverMore.default))
        }
    }
}
