// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.speed

import app.reseam.bundle.Logger
import app.reseam.youtube.core.showToast
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings

internal fun loadTapAndHoldSpeed(): Float {
    val speed = VideoSettings.playbackSpeedTapAndHold.value.toFloatOrNull()
    if (speed != null && speed > 0 && speed <= PLAYBACK_SPEED_MAXIMUM) return speed
    showInvalidSpeedToast()
    VideoSettings.playbackSpeedTapAndHold.reset()
    return VideoSettings.playbackSpeedTapAndHold.default.toFloat()
}

internal fun loadCustomSpeeds(): FloatArray =
    try {
        parseSpeeds(VideoSettings.customPlaybackSpeeds.value)
    } catch (exception: IllegalArgumentException) {
        Logger.info(exception) { "Invalid custom playback speeds" }
        showInvalidSpeedToast()
        VideoSettings.customPlaybackSpeeds.reset()
        parseSpeeds(VideoSettings.customPlaybackSpeeds.default)
    }

private fun parseSpeeds(raw: String): FloatArray {
    val values = raw.trim().replace(',', '.').split(Regex("\\s+"))
    val seen = mutableSetOf<Float>()
    return values
        .map { value ->
            val speed = value.toFloat()
            require(speed > 0 && speed <= PLAYBACK_SPEED_MAXIMUM && seen.add(speed)) { value }
            speed
        }
        .sorted()
        .toFloatArray()
}

private fun showInvalidSpeedToast() =
    showToast(YouTubeStrings.playbackSpeedInvalidSpeeds(PLAYBACK_SPEED_MAXIMUM.toString()))
