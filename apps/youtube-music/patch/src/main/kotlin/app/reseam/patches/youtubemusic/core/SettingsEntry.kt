// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.core

import app.reseam.patch.patch
import app.reseam.patch.settings.opensSettings
import app.reseam.patches.youtubecommon.exportFlagsFromSettingsRow
import app.reseam.patches.youtubecommon.openSettingsFromPreferenceIntents
import app.reseam.patches.youtubemusic.YouTubeMusicPatchStrings

val settingsEntry = patch(YouTubeMusicPatchStrings.settingsEntry) {
    description(YouTubeMusicPatchStrings.settingsEntryDescription)
    compatibleWith(YOUTUBE_MUSIC)
    dependsOn(youTubeMusicSettings)

    execute {
        openSettingsFromPreferenceIntents()
        exportFlagsFromSettingsRow()
        // androidx starts the intent of a preference that names no fragment.
        resources.editXml("xml", "settings_headers") {
            val row = createElement("Preference").apply {
                this["android:key"] = "reseam_settings"
                this["android:title"] = "Reseam Settings"
                this["android:order"] = "-1"
                this["android:persistent"] = "false"
                appendChild(
                    createElement("intent").apply { opensSettings() },
                )
            }
            val first = root.children.firstOrNull()
            if (first != null) root.insertBefore(row, first) else root.appendChild(row)
        }
    }
}
