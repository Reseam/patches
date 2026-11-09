// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.timeline

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.forceFlagWhen
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val showPollResults = patch("Show poll results") {
    description("Shows a poll's results before you vote.")
    compatibleWith(X)
    settings(xSettings, section(XSettingsPages.Home, XStrings.sectionTimeline, XSettings.showPollResults))

    execute {
        // A poll still open shows its results to voters and to its author, the only use of this flag.
        forceFlagWhen(", isAuthor=", XSettings.showPollResults, true)
    }
}
