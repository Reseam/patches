// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/** A label's mark, drawn on a 24-unit grid: a person for human, a robot for AI, a half-filled circle for mixed. */
internal class Glyph(label: Label?, color: Int, private val sizePx: Int) : Drawable() {
    private class Shape(val stroke: Path, val fill: Path)

    private val shape = when (label) {
        Label.Human -> HUMAN
        Label.Ai -> AI
        Label.Mixed, Label.AiAssisted -> MIXED
        null -> PENDING
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        this.color = color
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    override fun getIntrinsicWidth() = sizePx

    override fun getIntrinsicHeight() = sizePx

    override fun draw(canvas: Canvas) {
        val scale = bounds.width() / GRID
        canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(scale, scale)
        canvas.drawPath(shape.stroke, stroke)
        canvas.drawPath(shape.fill, fill)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        stroke.alpha = alpha
        fill.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        stroke.colorFilter = colorFilter
        fill.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT

    private companion object {
        const val GRID = 24f

        val HUMAN = Shape(
            stroke = Path().apply {
                addCircle(12f, 12f, 10f, Path.Direction.CW)
                addCircle(12f, 10f, 3f, Path.Direction.CW)
                moveTo(7f, 18.7f)
                cubicTo(8f, 16f, 16f, 16f, 17f, 18.7f)
            },
            fill = Path(),
        )
        val AI = Shape(
            stroke = Path().apply {
                addRoundRect(RectF(4f, 8f, 20f, 20f), 2f, 2f, Path.Direction.CW)
                moveTo(12f, 8f)
                lineTo(12f, 4f)
            },
            fill = Path().apply {
                addCircle(9f, 14f, 1.4f, Path.Direction.CW)
                addCircle(15f, 14f, 1.4f, Path.Direction.CW)
            },
        )
        val MIXED = Shape(
            stroke = Path().apply { addCircle(12f, 12f, 10f, Path.Direction.CW) },
            fill = Path().apply {
                moveTo(12f, 12f)
                arcTo(RectF(2f, 2f, 22f, 22f), 90f, 180f)
                close()
            },
        )
        val PENDING = Shape(stroke = Path(), fill = Path().apply { addCircle(12f, 12f, 3f, Path.Direction.CW) })
    }
}
