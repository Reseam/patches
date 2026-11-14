// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.FrameLayout
import app.reseam.bundle.resumedActivity

/** A label an app places in its own layout: a pill that follows its item's check. */
abstract class LabelView internal constructor(context: Context) : FrameLayout(context) {
    private val pill = Pill(context)
    private val observer = CheckObserver { render() }

    init {
        addView(pill)
        pill.setOnClickListener { context.activity()?.let(::onTap) }
        visibility = GONE
    }

    /** Null hides the label. Asked only while attached, since asking starts the check. */
    internal abstract fun look(): Look?

    internal abstract fun onTap(activity: Activity)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            pill.compact = pill.fullWidth + paddingLeft + paddingRight > MeasureSpec.getSize(widthMeasureSpec)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    protected fun render() {
        pill.show(if (isAttachedToWindow) look() else null)
        visibility = pill.visibility
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        CheckEvents.observe(observer)
        render()
    }

    override fun onDetachedFromWindow() {
        CheckEvents.stopObserving(observer)
        super.onDetachedFromWindow()
    }
}

private fun Context.activity(): Activity? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull() ?: resumedActivity()
