// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.privacy

import app.reseam.patches.instagram.core.GhostSettings
import app.reseam.patches.instagram.core.signatureCheckPatch
import app.reseam.patches.instagram.core.settingsPatch

import app.reseam.patch.compatibleWith
import app.reseam.patch.findMethod
import app.reseam.patch.findMethods
import app.reseam.patch.MethodHandle
import app.reseam.patch.parameterTypes
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.ToggleSetting

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
        skipMethodWhen(
            ctx.findMethod(debug = "typingIndicator") {
                strings("is_typing_indicator_enabled", "activityIndicatorSender")
                returnType("V")
            },
            GhostSettings.HideTyping,
            "typing indicator",
        )

        skipMethodWhen(
            ctx.findMethod(debug = "dmSeen") {
                strings("mark_thread_seen-")
                returnType("V")
            },
            GhostSettings.HideDmSeen,
            "DM read receipts",
        )

        skipMethodWhen(
            ctx.findMethod(debug = "storySeen") {
                strings("media/seen/")
                returnType("V")
            },
            GhostSettings.HideStorySeen,
            "story seen",
        )

        returnNullMethodWhen(
            ctx.findMethod(debug = "liveSeen") {
                strings("live/%s/heartbeat_and_get_viewer_count/")
            },
            GhostSettings.HideLiveSeen,
            "live seen heartbeat",
        )

        skipMethodWhen(
            ctx.findMethods(debug = "screenshotNotificationCandidates") {
                strings("ScreenshotNotificationManager")
                returnType("V")
            }.first { handle ->
                handle.method.info.parameterTypes.any { it.endsWith("Window;") }
            },
            GhostSettings.HideScreenshotNotifications,
            "screenshot notifications",
        )
    }
}

private fun skipMethodWhen(handle: MethodHandle, setting: ToggleSetting, label: String) {
    handle.skipWhen(setting)
}

private fun returnNullMethodWhen(handle: MethodHandle, setting: ToggleSetting, label: String) {
    handle.returnNullWhen(setting)
}
