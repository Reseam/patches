// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.instagram.download

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import app.reseam.bundle.dpToPx
import app.reseam.bundle.isNightMode
import app.reseam.instagram.settings.InstagramStrings

internal fun showCarouselDialog(
    context: Context?,
    totalSlides: Int,
    currentSlide: Int,
    onCurrent: () -> Unit,
    onAll: () -> Unit,
) {
    if (context == null) {
        onAll()
        return
    }
    val dark = context.resources.isNightMode()
    val bgColor = if (dark) 0xFF262626.toInt() else 0xFFFFFFFF.toInt()
    val textColor = if (dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
    val dividerColor = if (dark) 0xFF363636.toInt() else 0xFFEFEFEF.toInt()
    val handleColor = if (dark) 0xFF555555.toInt() else 0xFFC7C7C7.toInt()
    val dialog = Dialog(context)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    val root =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background =
                GradientDrawable().apply {
                    setColor(bgColor)
                    val radius = context.resources.dpToPx(14).toFloat()
                    cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                }
        }
    val handle =
        View(context).apply {
            background =
                GradientDrawable().apply {
                    setColor(handleColor)
                    cornerRadius = context.resources.dpToPx(2).toFloat()
                }
        }
    val handleParams =
        LinearLayout.LayoutParams(context.resources.dpToPx(36), context.resources.dpToPx(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = context.resources.dpToPx(8)
            bottomMargin = context.resources.dpToPx(12)
        }
    root.addView(handle, handleParams)
    if (currentSlide in 0 until totalSlides) {
        root.addView(
            row(
                context,
                InstagramStrings.downloadCarouselCurrent(currentSlide + 1, totalSlides),
                textColor,
            ) {
                dialog.dismiss()
                onCurrent()
            }
        )
        root.addView(divider(context, dividerColor))
    }
    root.addView(
        row(context, InstagramStrings.downloadCarouselAll(totalSlides), textColor) {
            dialog.dismiss()
            onAll()
        }
    )
    root.addView(divider(context, dividerColor))
    root.addView(row(context, InstagramStrings.downloadCancel, textColor) { dialog.dismiss() })
    root.setPadding(0, 0, 0, context.resources.dpToPx(8))
    dialog.setContentView(root)
    dialog.window?.apply {
        setBackgroundDrawable(GradientDrawable())
        setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        setGravity(Gravity.BOTTOM)
    }
    dialog.show()
}

private fun row(context: Context, label: String, textColor: Int, action: () -> Unit): View =
    TextView(context).apply {
        text = label
        setTextColor(textColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        gravity = Gravity.CENTER
        val padV = context.resources.dpToPx(16)
        setPadding(0, padV, 0, padV)
        layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        isClickable = true
        isFocusable = true
        val ripple = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)) {
            setBackgroundResource(ripple.resourceId)
        }
        setOnClickListener { action() }
    }

private fun divider(context: Context, color: Int): View =
    View(context).apply {
        setBackgroundColor(color)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
    }
