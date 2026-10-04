// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.core

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.patch
import app.reseam.patches.youtube.internal.booleanFeatureReads

private const val ROW_KEY = "reseam_settings"
private const val ROW_TITLE = "Reseam Settings"
private const val ICON = "reseam_settings_icon"
private const val ICON_PATH = "res/drawable/$ICON.xml"

/**
 * The row is an ordinary `<Preference>` in YouTube's own preference XML, so it gets the app's
 * styling for free, and androidx starts the `<intent>` a preference carries when it is tapped.
 */
val settingsEntry = patch("Reseam entry in YouTube settings") {
    description("Adds a Reseam Settings row to YouTube's settings screen.")
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

        // Read now, not from a constant: gmsCoreSupport renames the package before this runs.
        val target = manifest.packageName ?: error("the manifest declares no package")
        val icon = SettingsEntryResources::class.java.getResourceAsStream("/reseam-settings/$ICON.xml")
            ?.use { it.readBytes() }
            ?: error("reseam-settings/$ICON.xml is missing from the patch jar")
        resources.addFile("drawable", ICON, ICON_PATH, icon)
        resources.editXml("xml", "settings_fragment_cairo") {
            val group = root.children.firstOrNull { it.tag == "PreferenceCategory" } ?: root
            // Reuse the current screen's row layout.
            val iconRow = group.children.firstOrNull { it.tag == "Preference" && it["android:icon"] != null }
            group.appendChild(
                createElement("Preference").apply {
                    this["android:key"] = ROW_KEY
                    this["android:title"] = ROW_TITLE
                    if (iconRow != null) {
                        this["android:icon"] = "@drawable/$ICON"
                        this["android:layout"] = iconRow["android:layout"] ?: error("Settings icon row has no layout")
                        this["app:iconSpaceReserved"] = "true"
                    }
                    appendChild(
                        createElement("intent").apply {
                            this["android:targetPackage"] = target
                            this["android:targetClass"] = SETTINGS_ACTIVITY
                        },
                    )
                },
            )
        }
    }
}

private object SettingsEntryResources
