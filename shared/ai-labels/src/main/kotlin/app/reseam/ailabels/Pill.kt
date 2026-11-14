// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import app.reseam.ailabels.settings.AiLabelsStrings
import app.reseam.bundle.dpToPx
import app.reseam.runtime.ui.Icons
import app.reseam.runtime.ui.VectorIcon
import kotlin.math.roundToInt

/** A label's look: its mark, words and colors. */
internal data class Look(val label: Label?, val text: String, val fill: Int, val ink: Int)

/** Glyph, words and chevron in a rounded box: the label in a post header and on an image. */
internal class Pill(context: Context) : TextView(context) {
    private val gap = resources.dpToPx(GAP_DP)
    private val glyphSize = resources.dpToPx(GLYPH_DP)
    private val chevronSize = resources.dpToPx(CHEVRON_DP)
    private var shown: Look? = null

    /** The mark alone, for a box too narrow for the words. */
    var compact = false
        set(value) {
            if (field == value) return
            field = value
            shown?.let(::draw)
        }

    /** The width the pill takes with its words. */
    val fullWidth: Int
        get() = shown?.let { paddingLeft + paddingRight + glyphSize + chevronSize + 2 * gap + paint.measureText(it.text).roundToInt() } ?: 0

    init {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER_VERTICAL
        maxLines = 1
        val padding = resources.dpToPx(PADDING_DP)
        setPadding(padding, 0, padding, 0)
        minHeight = resources.dpToPx(HEIGHT_DP)
        visibility = GONE
    }

    fun show(look: Look?) {
        if (look == shown) return
        shown = look
        visibility = if (look == null) GONE else VISIBLE
        look?.let(::draw)
    }

    private fun draw(look: Look) {
        text = if (compact) null else look.text
        contentDescription = look.text
        setTextColor(look.ink)
        compoundDrawablePadding = if (compact) 0 else gap
        background = GradientDrawable().apply {
            cornerRadius = resources.dpToPx(RADIUS_DP).toFloat()
            setColor(look.fill)
        }
        setCompoundDrawablesRelativeWithIntrinsicBounds(
            Glyph(look.label, look.ink, glyphSize),
            null,
            if (compact) null else VectorIcon(Icons.CHEVRON_RIGHT, chevronSize, look.ink),
            null,
        )
    }

    private companion object {
        const val TEXT_SP = 12f
        const val HEIGHT_DP = 22
        const val PADDING_DP = 5
        const val GAP_DP = 2
        const val RADIUS_DP = 6
        const val GLYPH_DP = 12
        const val CHEVRON_DP = 10
    }
}

internal val Label.text: String
    get() = when (this) {
        Label.Human -> AiLabelsStrings.aiLabelHuman
        Label.Mixed -> AiLabelsStrings.aiLabelMixed
        Label.AiAssisted -> AiLabelsStrings.aiLabelAiAssisted
        Label.Ai -> AiLabelsStrings.aiLabelAi
    }

/** What a check that has no verdict yet says. */
internal fun CheckState<*>.waitingText(): String = when (this) {
    CheckState.SignedOut -> AiLabelsStrings.aiLabelSignIn
    CheckState.DailyLimit -> AiLabelsStrings.aiLabelLimit
    CheckState.NeedsConsent -> AiLabelsStrings.aiLabelAllowImages
    CheckState.Failed -> AiLabelsStrings.aiLabelRetry
    else -> AiLabelsStrings.aiLabelChecking
}
