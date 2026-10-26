// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.appEntry
import app.reseam.patch.settings.section
import app.reseam.patch.settings.settingsHost
import app.reseam.gmscore.settings.GmsCoreStrings
import app.reseam.patches.bundle.reseamAbout
import app.reseam.patches.bundle.reseamBrand
import app.reseam.youtube.core.bindings.YouTubeContext
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtubecommon.settings.YouTubeCommonSettings

val youTubeSettings = settingsHost("youtube") {
    compatibleWith(YOUTUBE)
    brand(reseamBrand)
    about(reseamAbout)
    strings(GmsCoreStrings)

    // Diagnostics belongs to the host rather than a feature patch.
    settings(section(YouTubeSettingsPages.Advanced, YouTubeStrings.sectionDiagnostics, YouTubeCommonSettings.debugLogging))

    install {
        // Extensions log and look up resources through this context from their first hook on.
        appEntry { YouTubeContext.init(application) }
    }
}
