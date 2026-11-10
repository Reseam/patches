// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.media

import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.instagram.settings.PlaybackSettings
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val disableAutoplay = patch(InstagramPatchStrings.disableAutoplay) {
    description(InstagramPatchStrings.disableAutoplayDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionPlayback, PlaybackSettings.disableVideoAutoplay))

    execute {
        gate(PlaybackSettings.disableVideoAutoplay) {
            autoplayGuard.alwaysReturn(true)
            autoplayDefault.alwaysReturn(true)
        }
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
