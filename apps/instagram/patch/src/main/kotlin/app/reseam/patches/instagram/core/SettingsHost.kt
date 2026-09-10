// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.core

import app.reseam.patch.appEntry
import app.reseam.patch.before
import app.reseam.patch.resourceRef
import app.reseam.patch.settings.settingsHost

private const val MAIN_ACTIVITY = "com.instagram.mainactivity.InstagramMainActivity"
private const val SETTINGS_ACTIVITY = "app.reseam.instagram.settings.InstagramReseamSettingsActivity"

// The activity gets no LAUNCHER intent filter: the runtime opens it from inside the app, and it
// stays out of the home screen's app drawer.
val instagramSettings = settingsHost("instagram") {
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)

    install {
        appEntry.before {
            call(InstagramSettingsEntry.init, thisObject)
        }

        val theme = manifest.edit {
            findByAttribute("android:name", MAIN_ACTIVITY).firstOrNull()?.get("android:theme")?.let(::resourceRef)
        } ?: 0x7f1400a0u
        manifest.addActivity(SETTINGS_ACTIVITY) {
            this["android:label"] = "Reseam Settings"
            setResourceRef("android:theme", theme)
        }
    }
}
