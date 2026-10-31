// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("RememberPlaybackSpeed")

package app.reseam.youtube.speed

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.video.VideoInformation

@Volatile private var newVideoStarted = false

@Export
fun newVideoStarted(controller: VideoInformation.PlaybackController) {
    newVideoStarted = true
}

@Export
fun applyDefaultPlaybackSpeed() {
    if (!newVideoStarted) return
    newVideoStarted = false
    val speed = defaultSpeed
    if (speed <= 0f) return
    VideoInformation.overridePlaybackSpeed(speed)
    Logger.debug { "Default playback speed applied: $speed" }
}

val defaultSpeed: Float
    get() =
        VideoSettings.playbackSpeedDefault.value.toFloatOrNull()?.takeIf {
            it > 0 && it <= PLAYBACK_SPEED_MAXIMUM
        } ?: 0f

@Export
fun userSelectedPlaybackSpeed(speed: Float) {
    if (!VideoSettings.rememberPlaybackSpeed.value) return
    VideoSettings.playbackSpeedDefault.value = speed.toString()
    Logger.debug { "Remembered playback speed: $speed" }
}
