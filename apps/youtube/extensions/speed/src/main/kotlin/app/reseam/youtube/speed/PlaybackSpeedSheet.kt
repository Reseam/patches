// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.speed

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import app.reseam.youtube.controls.adjustBrightness
import app.reseam.youtube.controls.createContent
import app.reseam.youtube.controls.createSheetDialog
import app.reseam.youtube.controls.dialogBackgroundColor
import app.reseam.youtube.controls.foregroundColor
import app.reseam.youtube.core.dp
import app.reseam.youtube.video.VideoInformation
import kotlin.math.ceil

private const val PRESET_COLUMNS = 5

internal fun showPlaybackSpeedSheet(context: Context) {
    val minimum = customPlaybackSpeeds.first()
    val maximum = customPlaybackSpeeds.last()
    val foreground = foregroundColor(context)
    val buttonColor = adjustBrightness(context, dialogBackgroundColor(context), 0.95f, 1.115f)
    val content = createContent(context)

    val currentSpeed =
        TextView(context).apply {
            text = formatSpeedX(VideoInformation.playbackSpeed)
            setTextColor(foreground)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams =
                LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    .apply { setMargins(0, dp(20f), 0, 0) }
        }
    content.addView(currentSpeed)
    val sliderRow =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
    val adjustments =
        SpeedAdjustment.entries.associateWith { adjustButton(context, it, foreground, buttonColor) }
    val slider =
        SeekBar(context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            max = toProgress(maximum, minimum)
            progress = toProgress(VideoInformation.playbackSpeed, minimum)
            progressDrawable.setColorFilter(foreground, PorterDuff.Mode.SRC_IN)
            thumb.setColorFilter(foreground, PorterDuff.Mode.SRC_IN)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
    sliderRow.addView(adjustments.getValue(SpeedAdjustment.DECREASE))
    sliderRow.addView(slider)
    sliderRow.addView(adjustments.getValue(SpeedAdjustment.INCREASE))
    content.addView(sliderRow)

    val select: (Float) -> Unit = { requested ->
        val speed = roundSpeed(requested)
        if (VideoInformation.playbackSpeed != speed) {
            currentSpeed.text = formatSpeedX(speed)
            slider.progress = toProgress(speed, minimum)
            userSelectedPlaybackSpeed(speed)
            VideoInformation.overridePlaybackSpeed(speed)
        }
    }
    slider.setOnSeekBarChangeListener(
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) select(minimum + progress / PROGRESS_SCALE)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
    )
    adjustments.forEach { (adjustment, button) ->
        button.setOnClickListener {
            select((VideoInformation.playbackSpeed + adjustment.delta).toFloat())
        }
    }

    val presets =
        GridLayout(context).apply {
            columnCount = PRESET_COLUMNS
            alignmentMode = GridLayout.ALIGN_BOUNDS
            rowCount = ceil(customPlaybackSpeeds.size / PRESET_COLUMNS.toDouble()).toInt()
            layoutParams =
                LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    .apply { setMargins(dp(4f), dp(12f), dp(4f), dp(12f)) }
            customPlaybackSpeeds.forEach {
                addView(presetCell(context, it, foreground, buttonColor, select))
            }
        }
    content.addView(presets)

    val dialog = createSheetDialog(context, content)
    dialog.show()
}

private fun toProgress(speed: Float, minimum: Float): Int =
    ((speed - minimum) * PROGRESS_SCALE).toInt()
