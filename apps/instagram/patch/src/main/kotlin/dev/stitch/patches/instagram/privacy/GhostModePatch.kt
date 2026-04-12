/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.patches.instagram.privacy

import dev.stitch.patches.instagram.core.GhostSettings
import dev.stitch.patches.instagram.core.signatureCheckPatch
import dev.stitch.patches.instagram.core.settingsPatch

import dev.stitch.patch.Fingerprint
import dev.stitch.patch.Method
import dev.stitch.patch.PatchRuntime
import dev.stitch.patch.compatibleWith
import dev.stitch.patch.fingerprint
import dev.stitch.patch.patch
import dev.stitch.patch.returnType
import dev.stitch.patch.settings.SettingsSection
import dev.stitch.patch.settings.ToggleSetting
import dev.stitch.patch.settings.returnNullWhen
import dev.stitch.patch.settings.skipWhen

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
