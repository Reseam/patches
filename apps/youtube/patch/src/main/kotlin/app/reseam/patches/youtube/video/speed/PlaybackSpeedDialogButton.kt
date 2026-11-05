// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.video.speed

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.playerControls
import app.reseam.patches.youtube.internal.registerPlayerControlIcon
import app.reseam.patches.youtube.internal.registerPlayerControlInitializer
import app.reseam.patches.youtube.internal.registerPlayerControlLayout
import app.reseam.patches.youtube.internal.userSelectedPlaybackSpeedHook
import app.reseam.patches.youtube.internal.videoInformationHook
import app.reseam.patches.youtube.internal.videoSpeedChangedHook
import app.reseam.youtube.settings.PlayerControlsSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.speed.bindings.PlaybackSpeedDialogButton

val playbackSpeedDialogButton = patch("Playback speed player button") {
    description("Adds a button that opens the configured playback-speed picker.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, playerControls, customPlaybackSpeed, videoInformationHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Controls, YouTubeStrings.sectionPlayerButtons, PlayerControlsSettings.playbackSpeedDialogButton))

    execute {
        resources.addId("reseam_playback_speed_button")
        registerPlayerControlIcon("reseam_playback_speed", "/buttons/reseam_playback_speed.xml", "/buttons/bold/reseam_text_box.xml")
        registerPlayerControlLayout("/buttons/playback_speed.xml", "/buttons/bold/playback_speed.xml",
            "reseam_playback_speed_button_container", position = 4)
        registerPlayerControlInitializer(PlaybackSpeedDialogButton.initialize)
        videoSpeedChangedHook(PlaybackSpeedDialogButton.videoSpeedChanged)
        userSelectedPlaybackSpeedHook(PlaybackSpeedDialogButton.videoSpeedChanged)
    }
}
