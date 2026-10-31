// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.instagram.settings

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import app.reseam.bundle.ReseamPatches
import app.reseam.bundle.dpToPx
import app.reseam.bundle.identifier

private const val LABEL = "Reseam Settings"
private const val BUTTON_TAG = 0x7f5151cf
private const val BUTTON_COLOR_ATTR = "igds_color_secondary_button_background_strong"
private const val CORNER_RADIUS_DP = 8

/** A full-width button under the Edit profile and Share profile buttons of the user's own profile. */
internal class ProfileSettingsButton(context: Context) {
    private val actionsRowId = context.identifier("profile_header_actions_top_row", "id")
    private val profileId = context.identifier("user_detail_fragment", "id")
    private val ownProfileMarkerId = context.identifier("profile_header_create_button", "id")

    fun attachTo(view: View) {
        if (actionsRowId == 0 || view.id != actionsRowId) return
        val row = view as ViewGroup
        // The row's own wrapper: the profile header around it is an adapter list that owns its children.
        val holder = row.parent as? LinearLayout ?: return
        if (holder.children().any { it.getTag(BUTTON_TAG) != null } || !isOwnProfile(row)) return
        // The row's buttons size themselves in a layout pass; until then there is nothing to copy.
        val template = row.getChildAt(0) ?: return
        if (template.height == 0) return
        holder.orientation = LinearLayout.VERTICAL
        holder.addView(button(template), holder.indexOfChild(row) + 1, layoutParams(row, template))
    }

    private fun isOwnProfile(row: View): Boolean {
        val profile = generateSequence(row.parent) { it.parent }.filterIsInstance<View>().firstOrNull { it.id == profileId }
        return profile?.findViewById<View>(ownProfileMarkerId) != null
    }

    // Instagram draws its buttons itself, so the surface comes from its theme and the text from the
    // Edit profile button.
    private fun button(template: View): TextView {
        val label = template.findLabel()
        return TextView(template.context).apply {
            setTag(BUTTON_TAG, true)
            text = LABEL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = resources.dpToPx(CORNER_RADIUS_DP).toFloat()
                setColor(context.themeColor(BUTTON_COLOR_ATTR))
            }
            label?.let {
                setTextColor(it.textColors)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, it.textSize)
                typeface = it.typeface
            }
            setOnClickListener { ReseamPatches.bundle().openSettings() }
        }
    }

    private fun layoutParams(row: ViewGroup, template: View): LinearLayout.LayoutParams {
        val gap = row.getChildAt(1)?.let { it.left - template.right } ?: 0
        return LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, template.height).apply {
            setMargins(template.left, gap, row.width - row.getChildAt(row.childCount - 1).right, 0)
        }
    }
}

private fun Context.themeColor(attr: String): Int {
    val value = TypedValue()
    theme.resolveAttribute(identifier(attr, "attr"), value, true)
    return value.data
}

private fun View.findLabel(): TextView? =
    this as? TextView ?: (this as? ViewGroup)?.children()?.firstNotNullOfOrNull { it.findLabel() }

private fun ViewGroup.children() = (0 until childCount).asSequence().map(::getChildAt)
