// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.media

import app.reseam.patches.instagram.core.MediaPlaybackSettings
import app.reseam.patches.instagram.core.signatureCheckPatch
import app.reseam.patches.instagram.core.settingsPatch

import app.reseam.patch.Fingerprint
import app.reseam.patch.PatchRuntime
import app.reseam.patch.compatibleWith
import app.reseam.patch.fingerprint
import app.reseam.patch.patch
import app.reseam.patch.returnType
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.ToggleSetting
import app.reseam.patch.settings.returnTrueWhen

private val autoplayGuardFingerprint = fingerprint {
    strings("ig_disable_video_autoplay", "ig_video_setting")
    returnType("Z")
}

private val autoplayDefaultFingerprint = fingerprint {
    strings("ig_autoplay_disabled_default")
    returnType("Z")
}

val disableAutoplayPatch = patch(
    name = "Disable video autoplay",
    description = "Stops feed and reels videos from auto-playing.",
    compatibleWith = listOf(compatibleWith("com.instagram.android")),
    settingsHost = settingsPatch,
    dependsOn = listOf(signatureCheckPatch, settingsPatch),
    settings = listOf(
        SettingsSection(
            title = "Playback",
            settings = listOf(MediaPlaybackSettings.DisableVideoAutoplay),
        ),
    ),
) {
    execute { ctx ->
        returnTrueWhen(ctx, autoplayGuardFingerprint, MediaPlaybackSettings.DisableVideoAutoplay, "autoplay guard")
        returnTrueWhen(ctx, autoplayDefaultFingerprint, MediaPlaybackSettings.DisableVideoAutoplay, "autoplay default")
    }
}

private fun returnTrueWhen(ctx: PatchRuntime, fp: Fingerprint, setting: ToggleSetting, label: String) {
    if (!fp.matched) {
        ctx.log.warn("$label fingerprint not matched")
        return
    }
    fp.method.returnTrueWhen(setting)
    ctx.log.info("Installed setting-controlled $label on '${setting.key}'")
}
