// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.app.Activity
import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.TextView
import app.reseam.bundle.ReseamPatches
import app.reseam.bundle.dpToPx
import app.reseam.runtime.ui.Tokens
import java.text.NumberFormat

/** A verdict's sheet: it rises from the bottom edge, in the host app's colors and the bundle's type. */
internal class Sheet(activity: Activity) {
    val ui = ReseamPatches.bundle().ui(activity)
    val context = ui.context
    val colors = palette(ui)
    private val dialog = Dialog(context)
    private val content = context.column {
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(px(Tokens.SPACE_XXL), px(Tokens.SPACE_MD), px(Tokens.SPACE_XXL), px(Tokens.SPACE_XXL))
        add(
            View(context).apply { background = ui.shape(colors.border, HANDLE_HEIGHT / 2f) },
            width = px(HANDLE_WIDTH),
            height = px(HANDLE_HEIGHT),
            bottom = Tokens.SPACE_LG,
        )
    }

    fun px(dp: Int): Int = context.resources.dpToPx(dp)

    /** [top] is dp, [height] pixels or a layout constant. */
    fun add(view: View, top: Int = 0, height: Int = WRAP_CONTENT) = content.add(view, height = height, top = top)

    fun header(verdict: Verdict, subtitle: String): View =
        context.row {
            add(glyphTile(verdict.label, TILE, TILE_RADIUS), width = px(TILE), height = px(TILE), end = Tokens.SPACE_MD)
            add(
                context.column {
                    add(ui.text(verdict.title, ui.type.titleMedium, colors.text).ellipsized(1))
                    add(ui.text(subtitle, ui.type.labelMedium, colors.secondaryText))
                },
                width = 0,
                weight = 1f,
            )
            add(ui.text(verdict.source, ui.type.labelMedium, colors.secondaryText), width = WRAP_CONTENT, start = Tokens.SPACE_SM)
        }

    fun glyphTile(label: Label, size: Int, radius: Float): View =
        View(context).apply {
            val tone = Tone.of(label)
            val glyph = px(GLYPH)
            val mark = InsetDrawable(Glyph(label, tone.ink(resources), glyph), (px(size) - glyph) / 2)
            background = LayerDrawable(arrayOf(ui.shape(tone.fill(resources), radius), mark))
        }

    fun card(padding: Int): LinearLayout =
        context.column {
            val border = colors.border
            background = ui.shape(colors.card, Tokens.RADIUS_LG.toFloat()).apply { setStroke(px(BORDER), border) }
            setPadding(px(padding), px(padding), px(padding), px(padding))
        }

    fun filledButton(label: String, onClick: () -> Unit): TextView =
        button(label, colors.onButton, ui.shape(colors.button, Tokens.RADIUS_SM.toFloat()), onClick)

    fun outlinedButton(label: String, onClick: () -> Unit): TextView {
        val border = colors.border
        val outline = ui.shape(colors.sheet, Tokens.RADIUS_SM.toFloat()).apply { setStroke(px(BORDER), border) }
        return button(label, colors.text, outline, onClick)
    }

    private fun button(label: String, ink: Int, shape: Drawable, onClick: () -> Unit): TextView =
        ui.text(label, ui.type.labelLarge, ink).apply {
            gravity = Gravity.CENTER
            minHeight = px(Tokens.BUTTON)
            background = shape
            setOnClickListener { onClick() }
        }

    fun dismiss() = dialog.dismiss()

    fun show() {
        val body = SheetScroll(context, dialog::dismiss).apply {
            background = ui.shape(colors.sheet, Tokens.RADIUS_XL.toFloat(), 0f)
            addView(content)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setOnApplyWindowInsetsListener { view, insets ->
                    view.setPadding(0, 0, 0, insets.getInsets(WindowInsets.Type.navigationBars()).bottom)
                    insets
                }
            }
        }
        with(dialog) {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(body)
            setCanceledOnTouchOutside(true)
            checkNotNull(window) { "A dialog always owns a window" }.apply {
                setBackgroundDrawable(ColorDrawable(0))
                setGravity(Gravity.BOTTOM)
                setLayout(MATCH_PARENT, WRAP_CONTENT)
                setWindowAnimations(android.R.style.Animation_InputMethod)
            }
            show()
        }
    }

    companion object {
        const val BORDER = 1
        private const val HANDLE_WIDTH = 32
        private const val HANDLE_HEIGHT = 4
        private const val TILE = 40
        private const val TILE_RADIUS = 12f
        private const val GLYPH = 22
    }
}

internal fun percent(share: Double): String = NumberFormat.getPercentInstance().format(share)
