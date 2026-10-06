// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubemusic.core

import app.reseam.patch.patch
import app.reseam.patch.settings.SETTINGS_OPEN_ACTION
import app.reseam.patches.youtubecommon.openSettingsFromPreferenceIntents

val settingsEntry = patch("Reseam entry in YouTube Music settings") {
    description("Adds a Reseam Settings row to YouTube Music's settings screen.")
    compatibleWith(YOUTUBE_MUSIC)
    dependsOn(youTubeMusicSettings)

    execute {
        openSettingsFromPreferenceIntents()
        // androidx starts the intent of a preference that names no fragment.
        resources.editXml("xml", "settings_headers") {
            val row = createElement("Preference").apply {
                this["android:key"] = "reseam_settings"
                this["android:title"] = "Reseam Settings"
                this["android:persistent"] = "false"
                appendChild(
                    createElement("intent").apply {
                        this["android:action"] = SETTINGS_OPEN_ACTION
                    },
                )
            }
            val about = root.children.firstOrNull { it["android:key"] == "settings_header_about_youtube_music" }
            if (about != null) root.insertBefore(row, about) else root.appendChild(row)
        }
    }
}
