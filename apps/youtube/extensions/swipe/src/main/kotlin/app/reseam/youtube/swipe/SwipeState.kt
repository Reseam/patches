// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.swipe

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.view.MotionEvent
import android.view.View
import app.reseam.youtube.player.PlayerType
import app.reseam.youtube.settings.PlayerControlsSettings
import app.reseam.youtube.settings.YouTubeStrings

internal class SwipeState(val activity: Activity) {
    private val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    val audio
        get() = checkNotNull(audioManager) { "The activity must provide an audio service" }

    val overlay =
        SwipeOverlay(activity).apply {
            visibility = View.GONE
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            isClickable = false
        }
    private val gesture = SwipeGesture(this)
    val forwarding
        get() = gesture.forwarding

    private data class BrightnessOverride(val original: Float, val saved: Float = -1f)

    private var brightnessOverride: BrightnessOverride? = null
    private val hide = Runnable { overlay.visibility = View.GONE }

    fun touch(event: MotionEvent): Boolean = gesture.touch(event)

    fun abortGesture() = gesture.abort()

    fun adjustGesture(dy: Float, adjustment: SwipeAdjustment) {
        val fraction = dy / (activity.window.decorView.height * .6f).coerceAtLeast(1f)
        when (adjustment) {
            is SwipeAdjustment.Volume -> {
                val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val sensitivity =
                    swipeNumber(PlayerControlsSettings.swipeVolumeSensitivity, 1f, .1f, 10f)
                setVolume(Math.round(adjustment.start + fraction * max * sensitivity))
            }
            is SwipeAdjustment.Brightness -> {
                val previous =
                    brightnessOverride
                        ?: BrightnessOverride(activity.window.attributes.screenBrightness)
                brightnessOverride = previous
                val value = (adjustment.start + fraction).coerceIn(0f, 1f)
                val brightness =
                    if (
                        value == 0f &&
                            PlayerControlsSettings.swipeLowestValueEnableAutoBrightness.value
                    )
                        -1f
                    else value
                setBrightness(brightness)
                brightnessOverride = previous.copy(saved = brightness)
                show(
                    SwipeFeedback(
                        YouTubeStrings.swipeOverlayBrightness,
                        brightness.coerceAtLeast(0f),
                        brightness < 0,
                        PlayerControlsSettings.swipeOverlayProgressBrightnessColor,
                    )
                )
            }
        }
    }

    fun setVolume(volume: Int) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val min =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                audio.getStreamMinVolume(AudioManager.STREAM_MUSIC)
            else 0
        val adjusted = volume.coerceIn(min, max)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, adjusted, 0)
        show(
            SwipeFeedback(
                YouTubeStrings.swipeOverlayVolume,
                if (max == 0) 0f else adjusted.toFloat() / max,
                false,
                PlayerControlsSettings.swipeOverlayProgressVolumeColor,
            )
        )
    }

    private fun setBrightness(value: Float) {
        val attributes = activity.window.attributes
        attributes.screenBrightness = value
        activity.window.attributes = attributes
    }

    fun playerChanged(type: PlayerType) {
        val fullscreen = type == PlayerType.WATCH_WHILE_FULLSCREEN
        if (!fullscreen) {
            gesture.cancel()
            swipeMain.removeCallbacks(hide)
            hide.run()
        }
        val brightness = brightnessOverride
        if (brightness != null && PlayerControlsSettings.swipeSaveAndRestoreBrightness.value) {
            setBrightness(if (fullscreen) brightness.saved else brightness.original)
        }
    }

    private fun show(feedback: SwipeFeedback) {
        overlay.feedback = feedback
        overlay.visibility = View.VISIBLE
        overlay.bringToFront()
        overlay.invalidate()
        swipeMain.removeCallbacks(hide)
        swipeMain.postDelayed(
            hide,
            swipeNumber(PlayerControlsSettings.swipeOverlayTimeout, 1000f, 100f, 10000f).toLong(),
        )
    }
}
