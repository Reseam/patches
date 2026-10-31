// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("SeekbarColor")

package app.reseam.youtube.theme

import android.graphics.Color
import app.reseam.bundle.Logger
import app.reseam.bundle.color
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.PlayerControlsSettings
import com.airbnb.lottie.LottieAnimationView

private val originalColor = 0xFFFF0000.toInt()
private val feedColors = intArrayOf(0xFFFF0033.toInt(), 0xFFFF2791.toInt())
private val feedPositions = floatArrayOf(0.8f, 1.0f)
private val hiddenGradient = intArrayOf(0, 0)
private val originalBrightness = hsv(originalColor)[2]
private val customEnabled = PlayerControlsSettings.seekbarCustomColor.value
private val hideThumbnail = PlayerControlsSettings.hideSeekbarThumbnail.value
private val customGradient =
    if (customEnabled)
        intArrayOf(
            PlayerControlsSettings.seekbarCustomColorPrimary.color,
            PlayerControlsSettings.seekbarCustomColorAccent.color,
        )
    else intArrayOf(originalColor, 0xFFFF2791.toInt())
private val customColor = customGradient[0]
private val customHsv = hsv(customColor)
private val splashColors = SplashColors(customColor, customGradient)

@Export
fun setSplashAnimationLottie(view: LottieAnimationView, resourceId: Int) =
    setSeekbarSplashAnimation(view, resourceId, splashColors.takeIf { customEnabled })

@Export
fun showWatchHistoryProgressDrawable(original: Boolean): Boolean = !hideThumbnail && original

@Export
fun getLithoColor(color: Int): Int =
    when {
        color != originalColor -> color
        hideThumbnail -> 0
        else -> customColor
    }

@Export
fun getPlayerLinearGradient(original: IntArray, x0: Int, y1: Int): IntArray =
    when {
        hideThumbnail && x0 == 0 && y1 == 0 -> hiddenGradient
        customEnabled -> customGradient
        else -> original
    }

@Export
fun getLithoLinearGradient(colors: IntArray, positions: FloatArray?): IntArray {
    if (!customEnabled && !hideThumbnail) return colors
    val isFeedGradient = feedColors.contentEquals(colors) && feedPositions.contentEquals(positions)
    return when {
        isFeedGradient -> if (hideThumbnail) hiddenGradient else customGradient
        else -> {
            Logger.debug {
                "Ignoring gradient colors: ${colors.contentToString()} positions: ${positions.contentToString()}"
            }
            colors
        }
    }
}

@Export
fun getVideoPlayerSeekbarClickedColor(color: Int): Int =
    if (customEnabled && color == originalColor) customColor else color

@Export
fun getVideoPlayerSeekbarColor(original: Int): Int =
    if (customEnabled) transformedColor(original) else original

private fun hsv(color: Int): FloatArray = FloatArray(3).also { Color.colorToHSV(color, it) }

private fun transformedColor(original: Int): Int = runCatching {
    val alphaDifference = Color.alpha(original) - Color.alpha(originalColor)
    val hsv =
        hsv(original).apply {
            val brightnessDifference = this[2] - originalBrightness
            this[0] = customHsv[0]
            this[1] = customHsv[1]
            this[2] = (customHsv[2] + brightnessDifference).coerceIn(0f, 1f)
        }
    val alpha = (Color.alpha(customColor) + alphaDifference).coerceIn(0, 255)
    Color.HSVToColor(alpha, hsv).also { replacement ->
        Logger.debug { "Seekbar color: ${original.hexColor()} -> ${replacement.hexColor()}" }
    }
}
    .getOrDefault(original)

private fun Int.hexColor(): String = "#${toUInt().toString(16).uppercase().padStart(8, '0')}"
