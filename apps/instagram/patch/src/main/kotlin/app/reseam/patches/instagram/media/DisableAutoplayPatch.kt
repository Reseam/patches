// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.media

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.returnTrueWhen
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.PlaybackSettings
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val disableAutoplay = patch("Disable video autoplay") {
    description("Stops feed and reels videos from auto-playing.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Playback", PlaybackSettings.disableVideoAutoplay))

    execute {
        autoplayGuard.returnTrueWhen(PlaybackSettings.disableVideoAutoplay)
        autoplayDefault.returnTrueWhen(PlaybackSettings.disableVideoAutoplay)
    }
}

val autoplayGuard = method("autoplayGuard") {
    strings("ig_disable_video_autoplay", "ig_video_setting")
    returns(Type.Boolean)
}

val autoplayDefault = method("autoplayDefault") {
    strings("ig_autoplay_disabled_default")
    returns(Type.Boolean)
}
