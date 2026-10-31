// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.swipe

import android.media.AudioManager
import android.view.MotionEvent
import app.reseam.bundle.Logger
import app.reseam.youtube.controls.PlayerControls
import app.reseam.youtube.core.debug
import app.reseam.youtube.player.EngagementPanel
import app.reseam.youtube.player.PlayerType
import app.reseam.youtube.settings.PlayerControlsSettings

internal data class SwipeOrigin(
    val x: Float,
    val y: Float,
    val time: Long,
    val adjustment: SwipeAdjustment,
)

internal sealed interface SwipeAdjustment {
    data class Volume(val start: Int) : SwipeAdjustment

    data class Brightness(val start: Float) : SwipeAdjustment
}

internal fun captureSwipeOrigin(
    owner: SwipeState,
    event: MotionEvent,
    density: Float,
): SwipeOrigin? {
    val decor = owner.activity.window.decorView
    val width = decor.width.toFloat()
    val height = decor.height.toFloat()
    val volumeZone = event.x > width * .625f
    // Engagement sheets retain ownership even after controls fade and with press-to-engage enabled.
    val insideHorizontalBounds = event.x > 20 * density && event.x < width - 20 * density
    val insideVerticalBounds = event.y > 40 * density && event.y < height - 80 * density
    val insideAdjustmentZone = volumeZone || event.x < width * .375f
    val eligible =
        when {
            !isSwipeFullscreen || EngagementPanel.openPanelId.isNotEmpty() -> false
            event.pointerCount != 1 -> false
            !insideHorizontalBounds || !insideVerticalBounds || !insideAdjustmentZone -> false
            !(if (volumeZone) PlayerControlsSettings.swipeVolume
                else PlayerControlsSettings.swipeBrightness)
                .value -> false
            else -> PlayerControlsSettings.swipePressToEngage.value || !PlayerControls.isVisible
        }
    Logger.debug {
        "Swipe gesture: ${PlayerType.current}, eligible=$eligible, controls=${PlayerControls.isVisible}"
    }
    val volume = owner.audio.getStreamVolume(AudioManager.STREAM_MUSIC)
    val screenBrightness = owner.activity.window.attributes.screenBrightness
    val brightness =
        if (screenBrightness < 0)
            android.provider.Settings.System.getInt(
                owner.activity.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                128,
            ) / 255f
        else screenBrightness
    return SwipeOrigin(
            event.x,
            event.y,
            event.eventTime,
            if (volumeZone) SwipeAdjustment.Volume(volume)
            else SwipeAdjustment.Brightness(brightness),
        )
        .takeIf { eligible }
}
