// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("PlaybackSpeedDialogButton")

package app.reseam.youtube.speed

import android.view.View
import app.reseam.runtime.Export
import app.reseam.youtube.controls.PlayerControlButton
import app.reseam.youtube.controls.playerControlButton
import app.reseam.youtube.settings.PlayerControlsSettings
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.video.VideoInformation

private var button: PlayerControlButton? = null

@Export
fun initialize(controls: View) {
    button =
        playerControlButton(
            controls,
            "reseam_playback_speed_button",
            containerId = "reseam_playback_speed_button_container",
            textId = "reseam_playback_speed_button_text",
            enabled = { PlayerControlsSettings.playbackSpeedDialogButton.value },
            longClick = {
                val default = defaultSpeed
                val resetToDefault =
                    VideoSettings.rememberPlaybackSpeed.value &&
                        default > 0 &&
                        VideoInformation.playbackSpeed != default
                VideoInformation.overridePlaybackSpeed(if (resetToDefault) default else 1f)
            },
        ) { view ->
            if (VideoSettings.restoreOldPlaybackSpeedMenu.value) showOldPlaybackSpeedMenu()
            else showModernDialog(view.context)
        }
    videoSpeedChanged(VideoInformation.playbackSpeed)
}

@Export
@Synchronized
fun videoSpeedChanged(speed: Float) {
    button?.setTextOverlay(formatSpeed(speed))
}
