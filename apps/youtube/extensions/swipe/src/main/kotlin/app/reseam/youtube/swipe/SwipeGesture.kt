// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.swipe

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import app.reseam.bundle.useReleasing
import app.reseam.youtube.player.EngagementPanel
import app.reseam.youtube.settings.PlayerControlsSettings
import kotlin.math.abs

internal class SwipeGesture(private val owner: SwipeState) {
    private enum class Phase {
        IDLE,
        PENDING,
        CAPTURED,
        DRAINING,
    }

    private val density = owner.activity.resources.displayMetrics.density
    private var phase = Phase.IDLE
    private lateinit var origin: SwipeOrigin
    var forwarding = false
        private set

    private val captured
        get() = phase == Phase.CAPTURED || phase == Phase.DRAINING

    private val eligible
        get() = phase == Phase.PENDING || phase == Phase.CAPTURED

    private val panelOpen
        get() = EngagementPanel.openPanelId.isNotEmpty()

    private val engagePress = Runnable {
        if (eligible && isSwipeFullscreen && !panelOpen) {
            MotionEvent.obtain(
                    origin.time,
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_CANCEL,
                    origin.x,
                    origin.y,
                    0,
                )
                .useReleasing(MotionEvent::recycle, ::captureGesture)
        }
    }

    fun abort() {
        phase = Phase.IDLE
    }

    fun cancel() {
        abort()
        swipeMain.removeCallbacks(engagePress)
    }

    fun touch(event: MotionEvent): Boolean {
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            beginGesture(event)
            return false
        }
        val ending = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
        if (ending || event.pointerCount > 1) {
            swipeMain.removeCallbacks(engagePress)
        }
        if (ending) {
            return captured.also { abort() }
        }
        if (!eligible) return captured
        if (!isSwipeFullscreen || panelOpen || event.pointerCount > 1) {
            // A cancelled downstream gesture must not receive an orphan UP.
            phase = if (captured) Phase.DRAINING else Phase.IDLE
            swipeMain.removeCallbacks(engagePress)
            return captured
        }
        if (action != MotionEvent.ACTION_MOVE) return captured
        val dx = event.x - origin.x
        val dy = origin.y - event.y
        if (!captured && !claimMovement(event, dx, dy)) return false
        if (abs(dy) < swipeNumber(PlayerControlsSettings.swipeThreshold, 30f, 5f, 200f) * density)
            return true
        owner.adjustGesture(dy, origin.adjustment)
        return true
    }

    private fun beginGesture(event: MotionEvent) {
        cancel()
        val start = captureSwipeOrigin(owner, event, density) ?: return
        origin = start
        phase = Phase.PENDING
        if (PlayerControlsSettings.swipePressToEngage.value) {
            swipeMain.postDelayed(engagePress, ViewConfiguration.getLongPressTimeout().toLong())
        }
    }

    private fun claimMovement(event: MotionEvent, dx: Float, dy: Float): Boolean {
        // Claim vertical motion before YouTube opens recommendations; the setting controls
        // adjustment.
        val threshold = ViewConfiguration.get(owner.activity).scaledTouchSlop.toFloat()
        if (abs(dx) > threshold && abs(dx) > abs(dy)) {
            cancel()
            return false
        }
        if (abs(dy) < threshold || abs(dy) <= abs(dx)) return false
        if (
            PlayerControlsSettings.swipePressToEngage.value &&
                event.eventTime - origin.time < ViewConfiguration.getLongPressTimeout()
        ) {
            cancel()
            return false
        }
        captureGesture(event)
        return true
    }

    private fun captureGesture(event: MotionEvent) {
        if (captured) return
        phase = Phase.CAPTURED
        MotionEvent.obtain(event)
            .apply { action = MotionEvent.ACTION_CANCEL }
            .useReleasing(MotionEvent::recycle) { cancel ->
                forwarding = true
                try {
                    owner.activity.dispatchTouchEvent(cancel)
                } finally {
                    forwarding = false
                }
            }
        if (PlayerControlsSettings.swipeHapticFeedback.value)
            owner.overlay.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
