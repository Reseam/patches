// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.privacy

import app.reseam.patches.instagram.core.GhostSettings
import app.reseam.patches.instagram.core.signatureCheckPatch
import app.reseam.patches.instagram.core.settingsPatch

import app.reseam.patch.compatibleWith
import app.reseam.patch.findMethod
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection

val ghostModePatch = patch(
    name = "Ghost mode",
    description = "Hides your activity: typing indicators, DM read receipts, story/live views, and screenshot notifications.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    settingsHost = settingsPatch,
    dependsOn = listOf(signatureCheckPatch, settingsPatch),
    settings = listOf(
        SettingsSection(
            title = "Ghost mode",
            settings = listOf(
                GhostSettings.HideTyping,
                GhostSettings.HideDmSeen,
                GhostSettings.HideStorySeen,
                GhostSettings.HideLiveSeen,
                GhostSettings.HideScreenshotNotifications,
            ),
        ),
    ),
) {
    execute { ctx ->
        ctx.findMethod(debug = "typingIndicator") {
            strings("is_typing_indicator_enabled", "activityIndicatorSender")
            returnType("V")
        }.skipWhen(GhostSettings.HideTyping)

        ctx.findMethod(debug = "dmSeen") {
            strings("mark_thread_seen-")
            returnType("V")
        }.skipWhen(GhostSettings.HideDmSeen)

        ctx.findMethod(debug = "storySeen") {
            strings("media/seen/")
            returnType("V")
        }.skipWhen(GhostSettings.HideStorySeen)

        ctx.findMethod(debug = "liveSeen") {
            strings("live/%s/heartbeat_and_get_viewer_count/")
        }.returnNullWhen(GhostSettings.HideLiveSeen)

        ctx.findMethod(debug = "screenshotNotificationManager") {
            strings("ScreenshotNotificationManager")
            returnType("V")
            hasParameter("Landroid/view/Window;")
        }.skipWhen(GhostSettings.HideScreenshotNotifications)
    }
}
