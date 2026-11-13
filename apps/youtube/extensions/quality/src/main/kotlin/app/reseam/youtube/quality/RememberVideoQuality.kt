// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("RememberVideoQuality")

package app.reseam.youtube.quality

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import app.reseam.bundle.Logger
import app.reseam.runtime.ChoiceValue
import app.reseam.runtime.Export
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.core.debug
import app.reseam.youtube.player.ShortsPlayer
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.video.VideoInformation
import app.reseam.youtube.video.VideoQuality

private const val SAME_AS_WIFI = "wifi"

private val onMobileData: Boolean
    get() {
        val connectivity =
            YouTubeContext.applicationContext.getSystemService(ConnectivityManager::class.java)
        return connectivity.getNetworkCapabilities(connectivity.activeNetwork)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }

private val wifiQualitySetting: ChoiceValue
    get() = if (ShortsPlayer.isActive) VideoSettings.shortsQualityDefault else VideoSettings.videoQualityDefault

private val networkQualitySetting: ChoiceValue
    get() =
        when {
            !onMobileData -> wifiQualitySetting
            ShortsPlayer.isActive -> VideoSettings.shortsQualityDefaultMobile
            else -> VideoSettings.videoQualityDefaultMobile
        }

val defaultQualityResolution: Int
    get() {
        val resolution =
            networkQualitySetting.value.takeUnless { it == SAME_AS_WIFI }
                ?: wifiQualitySetting.value
        return resolution.toIntOrNull() ?: VideoQuality.AUTOMATIC
    }

@Export
fun newVideoStarted(controller: VideoInformation.PlaybackController) {
    VideoQuality.setDesiredResolution { defaultQualityResolution }
}

@Export
fun userChangedQuality(resolution: Int) {
    if (!VideoSettings.rememberVideoQuality.value) return
    networkQualitySetting.value = resolution.toString()
    Logger.debug { "Remembered video quality: $resolution" }
}

@Export
fun userChangedShortsQuality(index: Int) {
    VideoQuality.available?.getOrNull(index)?.let {
        userChangedQuality(it.patch_getResolution())
    }
}
