// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PostedDate")

package app.reseam.pinterest.closeup

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import app.reseam.bundle.dpToPx
import app.reseam.pinterest.refs.createdAt
import app.reseam.pinterest.settings.PinterestStrings
import app.reseam.runtime.Export
import java.text.DateFormat

private const val LABEL_TAG = "reseam_posted_date"
private const val SIDE_PADDING_DP = 16
private const val TOP_PADDING_DP = 8

@Export
fun show(closeup: LinearLayout, pin: Any) {
    val label = closeup.findViewWithTag(LABEL_TAG) ?: label(closeup).also(closeup::addView)
    val posted = createdAt(pin)
    label.visibility = if (posted != null) View.VISIBLE else View.GONE
    label.text = posted?.let { PinterestStrings.postedOn(DateFormat.getDateInstance(DateFormat.MEDIUM).format(it)) }
}

private fun label(closeup: LinearLayout) = TextView(closeup.context).apply {
    tag = LABEL_TAG
    setTextAppearance(android.R.style.TextAppearance_Material_Small)
    resources.dpToPx(SIDE_PADDING_DP).let { side -> setPadding(side, resources.dpToPx(TOP_PADDING_DP), side, 0) }
}
