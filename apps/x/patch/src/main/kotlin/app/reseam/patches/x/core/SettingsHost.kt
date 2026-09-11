// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.settings.settingsHost

private const val SETTINGS_ACTIVITY = "app.reseam.x.settings.XReseamSettingsActivity"

val xSettings = settingsHost("x") {
    compatibleWith(X)

    install {
        appEntry.before {
            call(XSettingsEntry.init, thisObject)
        }
        // Opened from the Reseam row in X's settings (SettingsEntryPatch); no launcher entry.
        manifest.addActivity(SETTINGS_ACTIVITY) {
            this["android:label"] = "Reseam Settings"
        }
    }
}
