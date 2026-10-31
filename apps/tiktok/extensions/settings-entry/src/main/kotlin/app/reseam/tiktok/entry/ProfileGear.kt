// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ProfileGear")

package app.reseam.tiktok.entry

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import app.reseam.bundle.ReseamPatches
import app.reseam.runtime.Export
import com.bytedance.tux.icon.TuxIconView
import java.util.WeakHashMap

private const val KEY = "reseam_settings_gear"

// TikTok's server picks which of the two ≡ actions the signed-in profile shows.
private val MENU_KEYS = setOf("nav_bar_end_settings", "nav_bar_end_profile_menu")

private val navBarGears = WeakHashMap<View, View>()

/**
 * Puts TikTok's own gear [icon], opening Reseam settings, right before [anchor], once per parent.
 */
@Export
fun attach(anchor: View?, icon: Int) {
    val parent = anchor?.parent as? ViewGroup ?: return
    if (parent.findViewWithTag<View>(KEY) != null) return
    parent.addView(gear(anchor, icon), parent.indexOfChild(anchor))
}

/**
 * Adds the gear before ≡ as one of [navBar]'s own actions. The nav bar pairs its children with its
 * actions by position, so the gear has to be an action for TikTok to lay it out and update it.
 */
@Export
fun addToNavActions(navBar: View, start: MutableList<Any?>, end: MutableList<Any?>, icon: Int) {
    for (actions in listOf(start, end)) {
        val menu = actions.indexOfFirst { actionKey(it) in MENU_KEYS }
        if (menu < 0 || actions.any { actionKey(it) == KEY }) continue
        val anchor = actionView(actions[menu]) ?: continue
        actions.add(menu, viewAction(KEY, navBarGears.getOrPut(navBar) { gear(anchor, icon) }))
    }
}

@Export fun actionKey(action: Any?): Any? = null

@Export fun actionView(action: Any?): View? = null

@Export fun viewAction(key: String, view: View): Any? = null

@Export fun isDarkMode(context: Context): Boolean = false

private fun gear(anchor: View, icon: Int) =
    TuxIconView(anchor.context).apply {
        tag = KEY
        contentDescription = "Reseam Settings"
        setIconRes(icon)
        // TikTok's icons draw at the size they are given, so match the anchor's icon.
        (anchor as? ImageView)?.drawable?.let {
            setIconWidth(it.intrinsicWidth)
            setIconHeight(it.intrinsicHeight)
        }
        followTheme()
        setPadding(anchor.paddingLeft, anchor.paddingTop, anchor.paddingRight, anchor.paddingBottom)
        layoutParams = copy(anchor.layoutParams)
        setOnClickListener { ReseamPatches.bundle().openSettings() }
    }

private fun TuxIconView.followTheme() {
    var previousDarkMode: Boolean? = null
    val update = ViewTreeObserver.OnPreDrawListener {
        val darkMode = isDarkMode(context)

        if (darkMode != previousDarkMode) {
            previousDarkMode = darkMode
            setTintColor(if (darkMode) Color.WHITE else Color.BLACK)
        }

        true
    }

    // Use the activity's TikTok theme, including changes while the gear is attached.
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            view.viewTreeObserver.addOnPreDrawListener(update)
        }

        override fun onViewDetachedFromWindow(view: View) {
            view.viewTreeObserver.removeOnPreDrawListener(update)
        }
    })
}

private fun copy(params: ViewGroup.LayoutParams) =
    when (params) {
        is LinearLayout.LayoutParams -> LinearLayout.LayoutParams(params)
        is ViewGroup.MarginLayoutParams -> ViewGroup.MarginLayoutParams(params)
        else -> ViewGroup.LayoutParams(params)
    }
