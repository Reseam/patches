// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.quality

import android.content.Context
import android.graphics.PorterDuff
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import app.reseam.bundle.Logger
import app.reseam.bundle.identifier
import app.reseam.youtube.controls.adjustBrightness
import app.reseam.youtube.controls.createContent
import app.reseam.youtube.controls.createSheetDialog
import app.reseam.youtube.controls.foregroundColor
import app.reseam.youtube.core.debug
import app.reseam.youtube.core.dp
import app.reseam.youtube.video.VideoQuality

private data class QualityChoice(val quality: VideoQuality.Option, val label: String)

internal fun showQualitySheet(context: Context) {
    val qualities = VideoQuality.available
    val current = VideoQuality.current
    if (qualities == null || current == null || qualities.size < 2) {
        Logger.debug { "No qualities to show yet" }
        return
    }
    val choices =
        qualities
            .filterNotNull()
            .filter {
                it.patch_getResolution() != VideoQuality.AUTOMATIC
            }
            .map {
                QualityChoice(
                    it,
                    checkNotNull(it.patch_getQualityName()) { "YouTube quality has no name" },
                )
            }
    val selected = choices.indexOfLast { it.label == current.patch_getQualityName() }

    val foreground = foregroundColor(context)
    val dimmed = adjustBrightness(context, foreground, 1.6f, 0.6f)
    val content = createContent(context)
    val title =
        SpannableStringBuilder().apply {
            appendColored(youTubeString(context, "video_quality_quick_menu_title"), foreground)
            append("   ")
            appendColored(youTubeString(context, "video_quality_title_seperator"), dimmed)
            append("   ")
            appendColored(current.patch_getQualityName(), dimmed)
        }
    content.addView(
        TextView(context).apply {
            text = title
            textSize = 16f
            layoutParams =
                LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    .apply { setMargins(dp(12f), dp(16f), 0, dp(16f)) }
        }
    )
    val list =
        ListView(context).apply {
            adapter = QualityAdapter(context, choices, selected, foreground)
            divider = null
        }
    content.addView(list)

    val dialog = createSheetDialog(context, content)
    list.setOnItemClickListener { _, _, position, _ ->
        val quality = choices[position].quality
        userChangedQuality(quality.patch_getResolution())
        VideoQuality.change(quality)
        dialog.dismiss()
    }
    dialog.show()
}

private fun SpannableStringBuilder.appendColored(text: String?, color: Int) {
    val start = length
    append(text)
    setSpan(ForegroundColorSpan(color), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}

private fun youTubeString(context: Context, name: String): String =
    context.getString(context.identifier(name, "string"))

private class QualityAdapter(
    context: Context,
    choices: List<QualityChoice>,
    private val selected: Int,
    private val textColor: Int,
) : ArrayAdapter<QualityChoice>(context, 0, choices) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
            val check =
                ImageView(context).apply {
                    setImageResource(context.identifier("quantum_ic_check_white_24", "drawable"))
                    setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
                    visibility = if (position == selected) View.VISIBLE else View.INVISIBLE
                }
            addView(
                check,
                LinearLayout.LayoutParams(dp(24f), dp(24f)).apply { marginEnd = dp(24f) },
            )
            addView(
                TextView(context).apply {
                    text = getItem(position)?.label
                    setTextAppearance(android.R.style.TextAppearance_Material_Subhead)
                    setTextColor(textColor)
                }
            )
        }
}
