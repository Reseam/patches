// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.media

import app.reseam.patches.instagram.core.MediaPlaybackSettings
import app.reseam.patches.instagram.core.signatureCheckPatch
import app.reseam.patches.instagram.core.settingsPatch

import app.reseam.patch.compatibleWith
import app.reseam.patch.findMethod
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection

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
        ctx.findMethod(debug = "autoplayGuard") {
            strings("ig_disable_video_autoplay", "ig_video_setting")
            returnType("Z")
        }.returnTrueWhen(MediaPlaybackSettings.DisableVideoAutoplay)

        ctx.findMethod(debug = "autoplayDefault") {
            strings("ig_autoplay_disabled_default")
            returnType("Z")
        }.returnTrueWhen(MediaPlaybackSettings.DisableVideoAutoplay)
    }
}
