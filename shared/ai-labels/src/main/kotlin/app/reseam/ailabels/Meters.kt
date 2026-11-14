// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import app.reseam.bundle.dpToPx

/** A 270° ring split into AI, AI-assisted and human, in that order. */
internal class ShareRing(context: Context, shares: Shares) : View(context) {
    private class Arc(val start: Float, val sweep: Float, val color: Int)

    private val stroke = resources.dpToPx(STROKE_DP).toFloat()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }
    private val box = RectF()
    private val arcs: List<Arc> = run {
        val parts = listOf(shares.ai to ShareColors.AI, shares.assisted to ShareColors.ASSISTED, shares.human to ShareColors.HUMAN_RING)
            .filter { it.first >= MIN_SHARE }
        val total = parts.sumOf { it.first }
        val free = SWEEP - GAP * parts.size
        var angle = START
        parts.map { (share, color) ->
            val sweep = (free * share / total).toFloat()
            Arc(angle + GAP / 2, sweep, color).also { angle += sweep + GAP }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        box.set(stroke, stroke, w - stroke, h - stroke)
    }

    override fun onDraw(canvas: Canvas) {
        for (arc in arcs) {
            paint.color = arc.color
            canvas.drawArc(box, arc.start, arc.sweep, false, paint)
        }
    }

    private companion object {
        const val STROKE_DP = 12
        const val START = 135f
        const val SWEEP = 270f
        const val GAP = 14f
        const val MIN_SHARE = 0.005
    }
}

/**
 * Each scored stretch as a step at its verdict's height, as wide as its words: where in the text
 * the AI content sits.
 */
internal class StepChart(
    context: Context,
    private val segments: List<Segment>,
    private val levels: List<String>,
    private val axis: Pair<String, String>,
    textColor: Int,
    gridColor: Int,
) : View(context) {
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = resources.dpToPx(LABEL_SP).toFloat()
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = resources.dpToPx(LINE_DP).toFloat() }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grid = Paint().apply {
        color = gridColor
        strokeWidth = 1f
    }
    private val totalWords = segments.sumOf { it.words }.coerceAtLeast(1).toFloat()
    private val chart = RectF()
    private var rowHeight = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val gap = resources.dpToPx(GAP_DP).toFloat()
        chart.set(levels.maxOf(text::measureText) + gap, text.textSize / 2, w.toFloat(), h - text.textSize - gap)
        rowHeight = chart.height() / levels.size
    }

    override fun onDraw(canvas: Canvas) {
        levels.forEachIndexed { index, level ->
            val y = chart.top + rowHeight * index
            canvas.drawLine(chart.left, y, chart.right, y, grid)
            canvas.drawText(level, 0f, y + text.textSize / 2, text)
        }
        canvas.drawLine(chart.left, chart.bottom, chart.right, chart.bottom, grid)
        var x = chart.left
        for (segment in segments) {
            val step = chart.width() * segment.words / totalWords
            val top = chart.top + rowHeight * level(segment.label)
            val color = ShareColors.of(segment.label)
            fill.color = color and FILL_ALPHA
            canvas.drawRect(x, top, x + step, chart.bottom, fill)
            line.color = color
            canvas.drawLine(x, top, x + step, top, line)
            x += step
        }
        canvas.drawText(axis.first, chart.left, height.toFloat(), text)
        canvas.drawText(axis.second, chart.right - text.measureText(axis.second), height.toFloat(), text)
    }

    private fun level(label: Label): Int = when (label) {
        Label.Ai -> 0
        Label.Mixed, Label.AiAssisted -> 1
        Label.Human -> 2
    }.coerceAtMost(levels.size - 1)

    private companion object {
        const val LABEL_SP = 11
        const val LINE_DP = 3
        const val GAP_DP = 8
        const val FILL_ALPHA = 0x40FFFFFF
    }
}

/** Green to red from "not AI" to "AI", with a marker at [share]: the meter in an image verdict. */
internal class LikelihoodBar(context: Context, private val share: Float, marker: Int) : View(context) {
    private val radius = resources.dpToPx(RADIUS_DP).toFloat()
    private val inset = resources.dpToPx(MARKER_DP).toFloat()
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = marker }
    private val bar = RectF()
    private val markerBox = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        bar.set(inset, h * 0.2f, w - inset, h * 0.8f)
        barPaint.shader = LinearGradient(bar.left, 0f, bar.right, 0f, intArrayOf(HUMAN, NEUTRAL, AI), null, Shader.TileMode.CLAMP)
        val x = bar.left + bar.width() * share.coerceIn(0f, 1f)
        markerBox.set(x - inset, 0f, x + inset, h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(bar, radius, radius, barPaint)
        canvas.drawRoundRect(markerBox, inset, inset, markerPaint)
    }

    private companion object {
        const val RADIUS_DP = 6
        const val MARKER_DP = 3
        const val HUMAN = 0xFF2E9E6B.toInt()
        const val NEUTRAL = 0xFFF1EEE9.toInt()
        const val AI = 0xFFFF5A36.toInt()
    }
}
