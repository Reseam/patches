// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("RememberVideoQuality")

package app.reseam.youtube.quality

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.video.VideoInformation
import app.reseam.youtube.video.VideoQuality

val defaultQualityResolution: Int
    get() = VideoSettings.videoQualityDefault.value.toIntOrNull() ?: VideoQuality.AUTOMATIC

@Export
fun newVideoStarted(controller: VideoInformation.PlaybackController) {
    val resolution = defaultQualityResolution
    VideoQuality.setDesiredResolution(resolution)
    Logger.debug { "Default video quality applied: $resolution" }
}

@Export
fun userChangedQuality(resolution: Int) {
    if (!VideoSettings.rememberVideoQuality.value) return
    VideoSettings.videoQualityDefault.value = resolution.toString()
    Logger.debug { "Remembered video quality: $resolution" }
}

@Export
fun userChangedShortsQuality(index: Int) {
    VideoQuality.available?.getOrNull(index)?.let {
        userChangedQuality(it.patch_getResolution())
    }
}
