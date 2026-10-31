// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("VideoQualityDialogButton")

package app.reseam.youtube.quality

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.UnderlineSpan
import android.view.View
import app.reseam.bundle.Logger
import app.reseam.bundle.mainThreadHandler
import app.reseam.runtime.Export
import app.reseam.youtube.controls.PlayerControlButton
import app.reseam.youtube.controls.playerControlButton
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.PlayerControlsSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.video.VideoQuality

private const val TEXT_UPDATE_DELAY_MILLISECONDS = 100L
private var button: PlayerControlButton? = null
private var pendingText: CharSequence? = null
private val main = mainThreadHandler().also { VideoQuality.onChange.add(::updateText) }

@Export
fun initialize(controls: View) {
    button =
        playerControlButton(
            controls,
            "reseam_video_quality_button",
            containerId = "reseam_video_quality_button_container",
            textId = "reseam_video_quality_button_text",
            enabled = { PlayerControlsSettings.videoQualityDialogButton.value },
            longClick = { resetQuality(it.context) },
        ) {
            showQualitySheet(it.context)
        }
    updateText(VideoQuality.current)
}

private fun resetQuality(context: Context) {
    val qualities = VideoQuality.available ?: return
    val defaultResolution = defaultQualityResolution
    val quality =
        qualities.asSequence().filterNotNull().firstOrNull {
            val resolution = it.patch_getResolution()
            resolution != VideoQuality.AUTOMATIC && resolution <= defaultResolution
        }
    if (quality != null) {
        Logger.debug { "Resetting quality to: ${quality.patch_getQualityName()}" }
        VideoQuality.change(quality)
    } else {
        // The quality hook cannot select automatic quality.
        showQualitySheet(context)
    }
}

private fun updateText(quality: VideoQuality.Option?) {
    val resolution = quality?.patch_getResolution() ?: VideoQuality.AUTOMATIC
    val label =
        when (resolution) {
            VideoQuality.AUTOMATIC -> ""
            144,
            240,
            360 -> YouTubeStrings.videoQualityLabelLd
            480 -> YouTubeStrings.videoQualityLabelSd
            720 -> YouTubeStrings.videoQualityLabelHd
            1080 -> YouTubeStrings.videoQualityLabelFhd
            1440 -> YouTubeStrings.videoQualityLabelQhd
            2160 -> YouTubeStrings.videoQualityLabel4k
            else -> YouTubeStrings.videoQualityLabelUnknown
        }
    val text =
        SpannableStringBuilder(label).apply {
            if (VideoQuality.isPremium(quality)) {
                setSpan(UnderlineSpan(), 0, label.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    main.post {
        pendingText = text
        main.postDelayed(
            {
                if (pendingText === text) button?.setTextOverlay(text)
            },
            TEXT_UPDATE_DELAY_MILLISECONDS,
        )
    }
}
