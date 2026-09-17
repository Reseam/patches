// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.video.speed

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.playerControls
import app.reseam.patches.youtube.internal.registerPlayerControlInitializer
import app.reseam.patches.youtube.internal.registerPlayerControlLayout
import app.reseam.patches.youtube.internal.userSelectedPlaybackSpeedHook
import app.reseam.patches.youtube.internal.videoInformationHook
import app.reseam.patches.youtube.internal.videoSpeedChangedHook

object PlaybackSpeedDialogButton : ExtClass("app.reseam.youtube.speed.PlaybackSpeedDialogButton") {
    val initialize = static("initialize", Type.View)
    val videoSpeedChanged = static("videoSpeedChanged", Type.Float)
}

val playbackSpeedDialogButton = patch("Playback speed player button") {
    description("Adds a button that opens the configured playback-speed picker.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, playerControls, customPlaybackSpeed, videoInformationHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Controls, "Player buttons", YouTubeSettings.playbackSpeedDialogButton))

    execute {
        resources.addId("reseam_playback_speed_button")
        resources.addFile(
            "drawable",
            "reseam_playback_speed",
            "res/drawable/reseam_playback_speed.xml",
            PlaybackSpeedDialogButton::class.java.getResourceAsStream("/buttons/reseam_playback_speed.xml")!!.readBytes(),
        )
        registerPlayerControlLayout("/buttons/playback_speed.xml", "reseam_playback_speed_button_container", position = 4)
        registerPlayerControlInitializer(PlaybackSpeedDialogButton.initialize)
        videoSpeedChangedHook(PlaybackSpeedDialogButton.videoSpeedChanged)
        userSelectedPlaybackSpeedHook(PlaybackSpeedDialogButton.videoSpeedChanged)
    }
}
