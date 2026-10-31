// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.swipe

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import app.reseam.bundle.color
import app.reseam.runtime.TextValue
import app.reseam.youtube.settings.PlayerControlsSettings
import app.reseam.youtube.settings.YouTubeStrings

internal class SwipeOverlay(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var feedback =
        SwipeFeedback("", 0f, false, PlayerControlsSettings.swipeOverlayProgressBrightnessColor)

    override fun onDraw(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f
        val style = PlayerControlsSettings.swipeOverlayStyle.value
        if (style == "NONE") return
        val alpha =
            (swipeNumber(PlayerControlsSettings.swipeOverlayBackgroundOpacity, 70f, 0f, 100f) *
                    2.55f)
                .toInt()
        paint.color = Color.argb(alpha, 0, 0, 0)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(
            cx - 100 * density,
            cy - 65 * density,
            cx + 100 * density,
            cy + 65 * density,
            16 * density,
            16 * density,
            paint,
        )
        paint.color = feedback.color.color
        paint.textAlign = Paint.Align.CENTER
        paint.textSize =
            swipeNumber(PlayerControlsSettings.swipeTextOverlaySize, 22f, 10f, 48f) *
                resources.displayMetrics.scaledDensity
        canvas.drawText(feedback.label, cx, cy - 12 * density, paint)
        val progress =
            if (feedback.automatic) YouTubeStrings.swipeOverlayAuto
            else YouTubeStrings.swipeOverlayPercentage(Math.round(feedback.value * 100))
        canvas.drawText(progress, cx, cy + 18 * density, paint)
        when (style) {
            "CIRCULAR" -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4 * density
                canvas.drawArc(
                    cx - 55 * density,
                    cy - 55 * density,
                    cx + 55 * density,
                    cy + 55 * density,
                    -90f,
                    360 * feedback.value,
                    false,
                    paint,
                )
            }
            "PROGRESS" ->
                canvas.drawRect(
                    cx - 80 * density,
                    cy + 40 * density,
                    cx - 80 * density + 160 * density * feedback.value,
                    cy + 44 * density,
                    paint,
                )
        }
    }
}

internal data class SwipeFeedback(
    val label: String,
    val value: Float,
    val automatic: Boolean,
    val color: TextValue,
)
