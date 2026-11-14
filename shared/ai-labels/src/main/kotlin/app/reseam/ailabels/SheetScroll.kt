// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.ScrollView

/**
 * A sheet's scrolling body that also follows a downward drag from its top, and closes when let go far
 * enough down or with a downward fling.
 */
internal class SheetScroll(context: Context, private val onClose: () -> Unit) : ScrollView(context) {
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val fling = ViewConfiguration.get(context).scaledMinimumFlingVelocity * FLING_FACTOR
    private var startY = 0f
    private var dragging = false
    private var velocity: VelocityTracker? = null

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.rawY
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> if (startsDrag(event)) return true
        }
        return super.onInterceptTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!dragging && !(event.actionMasked == MotionEvent.ACTION_MOVE && startsDrag(event))) return super.onTouchEvent(event)
        val tracker = velocity ?: VelocityTracker.obtain().also { velocity = it }
        tracker.addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> translationY = (event.rawY - startY - slop).coerceAtLeast(0f)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                tracker.computeCurrentVelocity(MILLIS_PER_SECOND)
                val flung = event.actionMasked == MotionEvent.ACTION_UP && tracker.yVelocity > fling
                if (flung || translationY > height * CLOSE_FRACTION) {
                    animate().translationY(height.toFloat()).setDuration(SETTLE_MS).withEndAction(onClose).start()
                } else {
                    animate().translationY(0f).setDuration(SETTLE_MS).start()
                }
                tracker.recycle()
                velocity = null
                dragging = false
            }
        }
        return true
    }

    private fun startsDrag(event: MotionEvent): Boolean {
        if (dragging) return true
        dragging = scrollY == 0 && event.rawY - startY > slop
        return dragging
    }

    private companion object {
        const val CLOSE_FRACTION = 0.25f
        const val FLING_FACTOR = 8
        const val SETTLE_MS = 180L
        const val MILLIS_PER_SECOND = 1000
    }
}
