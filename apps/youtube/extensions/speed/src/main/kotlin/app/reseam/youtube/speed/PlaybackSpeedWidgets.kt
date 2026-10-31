// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.speed

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import app.reseam.bundle.identifier
import app.reseam.youtube.controls.roundedCorners
import app.reseam.youtube.core.dp

internal enum class SpeedAdjustment(val delta: Double) {
    DECREASE(-SPEED_ADJUSTMENT_CHANGE),
    INCREASE(SPEED_ADJUSTMENT_CHANGE),
}

internal fun presetCell(
    context: Context,
    speed: Float,
    foreground: Int,
    buttonColor: Int,
    select: (Float) -> Unit,
): View =
    FrameLayout(context).apply {
        layoutParams =
            GridLayout.LayoutParams().apply {
                width = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                setMargins(dp(4f), 0, dp(4f), 0)
                height = dp(60f)
            }
        addView(
            Button(context, null, 0).apply {
                text = formatSpeed(speed)
                setTextColor(foreground)
                textSize = 12f
                isAllCaps = false
                gravity = Gravity.CENTER
                background = rounded(buttonColor)
                setPadding(dp(4f), dp(4f), dp(4f), dp(4f))
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        dp(32f),
                        Gravity.CENTER,
                    )
                setOnClickListener { select(speed) }
            }
        )
        if (speed == 1f) {
            addView(
                TextView(context).apply {
                    setText(context.identifier("normal_playback_rate_label", "string"))
                    setTextColor(foreground)
                    textSize = 10f
                    gravity = Gravity.CENTER
                    layoutParams =
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                        )
                }
            )
        }
    }

internal fun adjustButton(
    context: Context,
    adjustment: SpeedAdjustment,
    foreground: Int,
    background: Int,
): Button =
    Button(context, null, 0).apply {
        text = ""
        this.background = rounded(background)
        this.foreground = SymbolDrawable(adjustment, foreground)
        layoutParams =
            LinearLayout.LayoutParams(dp(36f), dp(36f)).apply {
                setMargins(dp(8f), 0, dp(8f), 0)
            }
    }

private fun rounded(color: Int): ShapeDrawable =
    ShapeDrawable(RoundRectShape(roundedCorners(20f), null, null)).apply { paint.color = color }

private class SymbolDrawable(private val adjustment: SpeedAdjustment, color: Int) : Drawable() {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = dp(1f).toFloat()
        }

    override fun draw(canvas: Canvas) {
        val centerX = bounds.width() / 2f
        val centerY = bounds.height() / 2f
        val size = minOf(bounds.width(), bounds.height()) * 0.25f
        canvas.drawLine(centerX - size, centerY, centerX + size, centerY, paint)
        if (adjustment == SpeedAdjustment.INCREASE) {
            canvas.drawLine(centerX, centerY - size, centerX, centerY + size, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
