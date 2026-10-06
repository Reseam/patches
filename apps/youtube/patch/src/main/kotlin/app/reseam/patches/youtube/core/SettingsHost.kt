// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.settings.section
import app.reseam.patch.settings.settingsHost
import app.reseam.patches.youtubecommon.YouTubeCommonSettings
import app.reseam.patches.youtubecommon.YouTubeContext

val youTubeSettings = settingsHost("youtube") {
    compatibleWith(YOUTUBE)

    // Diagnostics belongs to the host rather than a feature patch.
    settings(section(YouTubeSettingsPages.Advanced, "Diagnostics", YouTubeCommonSettings.debugLogging))

    install {
        // Every extension reads settings and logs, so the context lands before any hooked code runs.
        appEntry { call(YouTubeContext.init, application) }
    }
}
