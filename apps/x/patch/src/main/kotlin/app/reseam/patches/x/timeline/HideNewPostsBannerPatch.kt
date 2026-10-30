// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.forceFlagWhen
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val hideNewPostsBanner = patch("Hide new posts banner") {
    description("Removes the new posts banner from the top of timelines.")
    compatibleWith(X)
    settings(xSettings, section(XStrings.sectionTimeline, XSettings.hideNewPostsBanner))

    execute {
        // The banner shows only while the timeline's instruction state allows it.
        forceFlagWhen(", isEligibleToShowPill=", XSettings.hideNewPostsBanner, false)
    }
}
