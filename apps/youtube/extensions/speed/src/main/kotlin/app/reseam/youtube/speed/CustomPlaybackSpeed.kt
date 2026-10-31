// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("CustomPlaybackSpeed")

package app.reseam.youtube.speed

import android.content.Context
import android.icu.text.NumberFormat
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.view.View
import android.view.ViewGroup
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings

// Past 8x playback gets no faster and YouTube's selector misbehaves.
@Export(name = "maximumSpeed") const val PLAYBACK_SPEED_MAXIMUM: Float = 8f
@Export(name = "minimumSpeed") const val SPEED_LIMIT_MINIMUM: Float = 0f
internal const val SPEED_ADJUSTMENT_CHANGE = 0.05
internal const val PROGRESS_SCALE = 100f
private const val OLD_MENU_DEBOUNCE_MILLISECONDS = 1000L

@Export @JvmField val customPlaybackSpeeds: FloatArray = loadCustomSpeeds()
private val configuredTapAndHoldSpeed = loadTapAndHoldSpeed()
private val multiplierFormat = speedFormat(minimumFractionDigits = 2)
private val labelFormat = speedFormat(minimumFractionDigits = 0)
private val buttonFormat = speedFormat(minimumFractionDigits = 1)
private const val TAP_AND_HOLD_CHIP = "seek_edu_overlay"
private val speedLabel = Regex("\\s*(\\d+(?:\\.\\d+)?)\\s*[x×]\\s*")
@Volatile private var lastOldMenuTime = 0L

@Export fun customPlaybackSpeedCount(): Int = customPlaybackSpeeds.size

@Export fun getTapAndHoldSpeed(): Float = configuredTapAndHoldSpeed

@Export
fun onLithoTextLoaded(path: StringBuilder?, text: CharSequence?): CharSequence? {
    if (path == null || text == null || path.indexOf(TAP_AND_HOLD_CHIP) < 0) return text
    val number = speedLabel.matchEntire(text)?.groups?.get(1) ?: return text
    // Change only the number to retain YouTube's styling spans.
    return SpannableStringBuilder(text)
        .replace(
            number.range.first,
            number.range.last + 1,
            labelFormat.format(configuredTapAndHoldSpeed),
        )
}

@Export
fun onFlyoutMenuCreate(recyclerView: View) {
    val menu = recyclerView as? ViewGroup ?: return
    menu.viewTreeObserver.addOnDrawListener {
        val restoreOldMenu = VideoSettings.restoreOldPlaybackSpeedMenu.value
        PlaybackMenu.entries.forEach {
            if (restoreOldMenu) it.visible = false else it.replaceIfVisible(menu)
        }
    }
}

fun showOldPlaybackSpeedMenu() {
    val now = SystemClock.elapsedRealtime()
    if (now - lastOldMenuTime < OLD_MENU_DEBOUNCE_MILLISECONDS) {
        Logger.debug { "Ignoring a repeated old playback speed menu call" }
        return
    }
    lastOldMenuTime = now
    openOldPlaybackSpeedMenu()
}

// The patch writes the body: it calls the old speed menu it captured.
@Export private fun openOldPlaybackSpeedMenu() {}

fun showModernDialog(context: Context) = showPlaybackSpeedSheet(context)

internal fun formatSpeedX(speed: Float): String =
    YouTubeStrings.playbackSpeedMultiplier(multiplierFormat.format(speed))

internal fun formatSpeed(speed: Float): String = buttonFormat.format(speed)

private fun speedFormat(minimumFractionDigits: Int) =
    NumberFormat.getNumberInstance().apply {
        this.minimumFractionDigits = minimumFractionDigits
        maximumFractionDigits = 2
    }

internal fun roundSpeed(speed: Float): Float {
    if (customPlaybackSpeeds.any { it == speed }) return speed
    // Double arithmetic keeps values such as 1.15 from rounding down to 1.1.
    val rounded = (Math.round(speed / SPEED_ADJUSTMENT_CHANGE) * SPEED_ADJUSTMENT_CHANGE).toFloat()
    return rounded.coerceIn(SPEED_ADJUSTMENT_CHANGE.toFloat(), PLAYBACK_SPEED_MAXIMUM)
}
