// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.privacy

import app.reseam.patches.instagram.core.GhostSettings
import app.reseam.patches.instagram.core.signatureCheckPatch
import app.reseam.patches.instagram.core.settingsPatch

import app.reseam.patch.Fingerprint
import app.reseam.patch.Method
import app.reseam.patch.PatchRuntime
import app.reseam.patch.compatibleWith
import app.reseam.patch.fingerprint
import app.reseam.patch.patch
import app.reseam.patch.returnType
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.returnNullWhen
import app.reseam.patch.settings.skipWhen

private val typingIndicatorFingerprint = fingerprint {
    strings("is_typing_indicator_enabled", "activityIndicatorSender")
    returnType("V")
}

private val dmSeenFingerprint = fingerprint {
    strings("mark_thread_seen-")
    returnType("V")
}

private val storySeenFingerprint = fingerprint {
    strings("media/seen/")
    returnType("V")
}

private val liveSeenFingerprint = fingerprint {
    strings("live/%s/heartbeat_and_get_viewer_count/")
}

private val screenshotNotificationFingerprint = fingerprint {
    strings("ScreenshotNotificationManager")
    returnType("V")
    custom { parameterTypes.any { it.endsWith("Window;") } }
}

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
        skipMethodWhen(ctx, typingIndicatorFingerprint, GhostSettings.HideTyping, "typing indicator")
        skipMethodWhen(ctx, dmSeenFingerprint, GhostSettings.HideDmSeen, "DM read receipts")
        skipMethodWhen(ctx, storySeenFingerprint, GhostSettings.HideStorySeen, "story seen")
        returnNullMethodWhen(ctx, liveSeenFingerprint, GhostSettings.HideLiveSeen, "live seen heartbeat")
        skipMethodWhen(ctx, screenshotNotificationFingerprint, GhostSettings.HideScreenshotNotifications, "screenshot notifications")
    }
}

private fun skipMethodWhen(ctx: PatchRuntime, fp: Fingerprint, setting: ToggleSetting, label: String) {
    if (!fp.matched) {
        ctx.log.warn("$label fingerprint not matched")
        return
    }
    fp.method.skipWhen(setting)
    ctx.log.info("Installed setting-controlled $label on '${setting.key}'")
}

private fun returnNullMethodWhen(ctx: PatchRuntime, fp: Fingerprint, setting: ToggleSetting, label: String) {
    if (!fp.matched) {
        ctx.log.warn("$label fingerprint not matched")
        return
    }
    val m: Method = fp.method
    m.returnNullWhen(setting)
    ctx.log.info("Installed setting-controlled $label on '${setting.key}'")
}
