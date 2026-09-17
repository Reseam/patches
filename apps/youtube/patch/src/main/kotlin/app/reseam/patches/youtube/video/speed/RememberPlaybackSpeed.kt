// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.video.speed

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.onCreateHook
import app.reseam.patches.youtube.internal.userSelectedPlaybackSpeedHook
import app.reseam.patches.youtube.internal.videoInformationHook

object RememberPlaybackSpeed : ExtClass("app.reseam.youtube.speed.RememberPlaybackSpeed") {
    val newVideoStarted = static("newVideoStarted", "app.reseam.youtube.video.VideoInformation\$PlaybackController")
    val applyDefaultPlaybackSpeed = static("applyDefaultPlaybackSpeed")
    val userSelectedPlaybackSpeed = static("userSelectedPlaybackSpeed", Type.Float)
}

val rememberPlaybackSpeed = patch("Remember playback speed") {
    description("Applies a default playback speed and remembers explicit selections.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoInformationHook, customPlaybackSpeed)
    settings(youTubeSettings, section(YouTubeSettingsPages.Video, "Playback speed", YouTubeSettings.playbackSpeedDefault, YouTubeSettings.rememberPlaybackSpeed))

    execute {
        onCreateHook(RememberPlaybackSpeed.newVideoStarted)
        userSelectedPlaybackSpeedHook(RememberPlaybackSpeed.userSelectedPlaybackSpeed)
        oldPlaybackSpeedMenu.before { call(RememberPlaybackSpeed.applyDefaultPlaybackSpeed) }
    }
}
