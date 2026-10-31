// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.controls

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.Scroller

internal class DragLayout(
    context: Context,
    private val dialog: SheetDialog,
    private val animationDuration: Int,
) : LinearLayout(context) {
    private val scroller = Scroller(context, DecelerateInterpolator())
    private lateinit var velocityTracker: VelocityTracker
    private val settle = Runnable { settle() }
    private var initialTouchY = 0f
    private var dragOffset = 0f

    private enum class DragState {
        ENTERING,
        IDLE,
        DRAGGING,
    }

    private var state = DragState.ENTERING
    private val dismissThreshold
        get() = height * 0.5f

    init {
        isClickable = true
        postDelayed({ state = DragState.IDLE }, (animationDuration + 50).toLong())
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (state == DragState.ENTERING) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchY = event.rawY
                state = DragState.IDLE
                scroller.forceFinished(true)
                removeCallbacks(settle)
                velocityTracker.clear()
                velocityTracker.addMovement(event)
                dragOffset = translationY
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.rawY - initialTouchY
                if (
                    dy > ViewConfiguration.get(context).scaledTouchSlop && !canChildScrollUp(this)
                ) {
                    state = DragState.DRAGGING
                    return true
                }
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (state == DragState.ENTERING) return super.onTouchEvent(event)
        velocityTracker.addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE ->
                if (state == DragState.DRAGGING) {
                    dragOffset = maxOf(0f, event.rawY - initialTouchY)
                    translationY = dragOffset
                }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker.computeCurrentVelocity(1000)
                val shouldDismiss =
                    event.actionMasked != MotionEvent.ACTION_CANCEL &&
                        (dragOffset > dismissThreshold || velocityTracker.yVelocity > 800)
                val target = if (shouldDismiss) height else 0
                scroller.startScroll(
                    0,
                    dragOffset.toInt(),
                    0,
                    target - dragOffset.toInt(),
                    animationDuration,
                )
                postOnAnimation(settle)
                state = DragState.IDLE
            }
        }
        return true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        velocityTracker = VelocityTracker.obtain()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(settle)
        scroller.forceFinished(true)
        velocityTracker.recycle()
        super.onDetachedFromWindow()
    }

    private fun settle() {
        if (!scroller.computeScrollOffset()) {
            dragOffset = translationY
            return
        }
        dragOffset = scroller.currY.toFloat()
        translationY = dragOffset
        if (dragOffset >= height) {
            dialog.dismiss()
            scroller.forceFinished(true)
        } else {
            postOnAnimation(settle)
        }
    }
}

private fun canChildScrollUp(group: ViewGroup): Boolean =
    (0 until group.childCount).any { index ->
        val child = group.getChildAt(index)
        child.canScrollVertically(-1) || child is ViewGroup && canChildScrollUp(child)
    }
