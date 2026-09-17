// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.settings.section
import app.reseam.patch.settings.settingsHost

internal const val SETTINGS_ACTIVITY = "app.reseam.youtube.core.YouTubeReseamSettingsActivity"

val youTubeSettings = settingsHost("youtube") {
    compatibleWith(YOUTUBE)

    // Diagnostics belongs to the host rather than a feature patch.
    settings(section(YouTubeSettingsPages.Advanced, "Diagnostics", YouTubeSettings.debugLogging))

    install {
        // Every extension reads settings and logs, so the context lands before any hooked code runs.
        appEntry.before { call(YouTubeContext.init, thisObject) }
        // Opened from the Reseam row in YouTube's settings (SettingsEntryPatch); no launcher entry.
        manifest.addActivity(SETTINGS_ACTIVITY) {
            this["android:label"] = "Reseam Settings"
        }
    }
}
