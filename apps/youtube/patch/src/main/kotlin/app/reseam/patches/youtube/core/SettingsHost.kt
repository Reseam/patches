// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.settings.section
import app.reseam.patch.settings.settingsHost

internal const val SETTINGS_ACTIVITY = "app.reseam.youtube.core.YouTubeReseamSettingsActivity"

// The row launches the activity by this action, not by an explicit component, so a later package
// rename (Change package name) cannot leave the row pointing at a package that no longer exists.
// The activity is not exported, so only this app resolves the action; a coexisting clone cannot.
internal const val SETTINGS_ACTION = "app.reseam.youtube.SETTINGS"

val youTubeSettings = settingsHost("youtube") {
    compatibleWith(YOUTUBE)

    // Diagnostics belongs to the host rather than a feature patch.
    settings(section(YouTubeSettingsPages.Advanced, "Diagnostics", YouTubeSettings.debugLogging))

    install {
        // Every extension reads settings and logs, so the context lands before any hooked code runs.
        appEntry { call(YouTubeContext.init, application) }
        // Opened from the Reseam row in YouTube's settings (SettingsEntryPatch); no launcher entry.
        manifest.addActivity(SETTINGS_ACTIVITY) {
            this["android:label"] = "Reseam Settings"
        }
        manifest.addIntentFilter(SETTINGS_ACTIVITY, action = SETTINGS_ACTION, category = "android.intent.category.DEFAULT")
    }
}
