// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.forceFlagWhen
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val hideNewPostsBanner = patch(XPatchStrings.hideNewPostsBanner) {
    description(XPatchStrings.hideNewPostsBannerDescription)
    compatibleWith(X)
    settings(xSettings, section(XSettingsPages.Home, XStrings.sectionTimeline, XSettings.hideNewPostsBanner))

    execute {
        forceFlagWhen(", isEligibleToShowPill=", XSettings.hideNewPostsBanner, false)
    }
}
