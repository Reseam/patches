// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import app.reseam.bundle.dpToPx

internal fun Context.column(block: LinearLayout.() -> Unit): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        block()
    }

internal fun Context.row(block: LinearLayout.() -> Unit): LinearLayout =
    LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        block()
    }

/** [width] and [height] are pixels or a layout constant; the margins are dp. */
internal fun LinearLayout.add(
    view: View,
    width: Int = MATCH_PARENT,
    height: Int = WRAP_CONTENT,
    weight: Float = 0f,
    top: Int = 0,
    bottom: Int = 0,
    start: Int = 0,
    end: Int = 0,
) {
    addView(
        view,
        LinearLayout.LayoutParams(width, height, weight).apply {
            topMargin = resources.dpToPx(top)
            bottomMargin = resources.dpToPx(bottom)
            marginStart = resources.dpToPx(start)
            marginEnd = resources.dpToPx(end)
        },
    )
}

internal fun TextView.ellipsized(lines: Int): TextView = apply {
    maxLines = lines
    ellipsize = TextUtils.TruncateAt.END
}
