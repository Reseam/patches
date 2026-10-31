// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.reseam.youtube.core.dp
import app.reseam.youtube.settings.YouTubeStrings
import java.util.Locale

// The window overlay leaves touch handling and the player's layout untouched.
internal class ScrubbingPreviewCard(val context: Context) {
    private val frame =
        ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider =
                object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, previewCorner.toFloat())
                    }
                }
        }
    private val time =
        TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(8f), dp(2f), dp(8f), dp(2f))
            background =
                GradientDrawable().apply {
                    setColor(0xB3000000.toInt())
                    cornerRadius = dp(12f).toFloat()
                }
        }
    val root =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(frame)
            addView(
                time,
                LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    .apply { topMargin = dp(4f) },
            )
        }
    private var host: ViewGroup? = null
    private var shown: Bitmap? = null
    val hasFrame: Boolean
        get() = shown != null

    fun setFrame(bitmap: Bitmap) {
        if (bitmap === shown) return
        shown = bitmap
        frame.setImageBitmap(bitmap)
        val width = bitmap.width
        val height = bitmap.height
        frame.layoutParams =
            if (width >= height) {
                LinearLayout.LayoutParams(previewLongSide, previewLongSide * height / width)
            } else {
                LinearLayout.LayoutParams(previewLongSide * width / height, previewLongSide)
            }
    }

    fun setTime(millis: Long) {
        val seconds = millis / 1000
        val locale = Locale.getDefault()
        time.text =
            if (seconds >= 3600) {
                YouTubeStrings.scrubbingPreviewTimeHours(
                    String.format(locale, "%d", seconds / 3600),
                    String.format(locale, "%02d", seconds / 60 % 60),
                    String.format(locale, "%02d", seconds % 60),
                )
            } else {
                YouTubeStrings.scrubbingPreviewTimeMinutes(
                    String.format(locale, "%d", seconds / 60),
                    String.format(locale, "%02d", seconds % 60),
                )
            }
    }

    fun attach(target: ViewGroup) {
        if (host !== target) {
            detach()
            target.overlay.add(root)
            host = target
        }
        val unbounded = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        root.measure(unbounded, unbounded)
    }

    fun detach() {
        host?.overlay?.remove(root)
        host = null
        shown = null
        frame.setImageBitmap(null)
    }
}

// Clears the most-replayed graph drawn over the seekbar while scrubbing.
internal val previewGap = dp(28f)
private val previewLongSide = dp(160f)
private val previewCorner = dp(8f)
