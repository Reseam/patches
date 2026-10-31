// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.patch
import app.reseam.patch.settings.opensSettings
import app.reseam.patches.youtube.internal.booleanFeatureReads
import app.reseam.patches.youtubecommon.exportFlagsFromSettingsRow
import app.reseam.patches.youtubecommon.openSettingsFromPreferenceIntents

private const val ROW_KEY = "reseam_settings"
private const val ROW_TITLE = "Reseam Settings"
private const val ICON = "reseam_settings_icon"
private const val ICON_PATH = "res/drawable/$ICON.xml"

/**
 * The row is an ordinary `<Preference>` in YouTube's own preference XML, so it gets the app's
 * styling for free. androidx starts the `<intent>` a preference carries when it is tapped; the
 * row's intent asks for the settings, which open over the screen rather than in an activity.
 */
val settingsEntry = patch("Reseam entry in YouTube settings") {
    description("Adds a Reseam Settings row to YouTube's settings screen. Long-pressing it saves the app's experiment flags to Downloads.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)

    execute {
        // YouTube always inflates the grouped Cairo screen, even before remote flags load.
        // Its legacy path cannot remove nested rows and leaves blank, unstyled preferences.
        booleanFeatureReads(45532100L).forEach { site ->
            site.next { resultOf(Type.Boolean) }.captureAs("enabled", Type.Boolean).after {
                capture("enabled").assign(bool(true))
            }
        }

        openSettingsFromPreferenceIntents()
        exportFlagsFromSettingsRow()

        val icon = SettingsEntryResources::class.java.getResourceAsStream("/reseam-settings/$ICON.xml")
            ?.use { it.readBytes() }
            ?: error("reseam-settings/$ICON.xml is missing from the patch jar")
        resources.addFile("drawable", ICON, ICON_PATH, icon)
        resources.editXml("xml", "settings_fragment_cairo") {
            val group = root.children.firstOrNull { it.tag == "PreferenceCategory" } ?: root
            // Reuse the current screen's row layout.
            val iconRow = group.children.firstOrNull { it.tag == "Preference" && it["android:icon"] != null }
            val row = createElement("Preference").apply {
                this["android:key"] = ROW_KEY
                this["android:title"] = ROW_TITLE
                this["android:order"] = "-1"
                if (iconRow != null) {
                    this["android:icon"] = "@drawable/$ICON"
                    this["android:layout"] = iconRow["android:layout"] ?: error("Settings icon row has no layout")
                    this["app:iconSpaceReserved"] = "true"
                }
                appendChild(
                    createElement("intent").apply { opensSettings() },
                )
            }
            val first = group.children.firstOrNull()
            if (first != null) group.insertBefore(row, first) else group.appendChild(row)
        }
    }
}

private object SettingsEntryResources
