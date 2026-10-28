// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("WideSearchBar")

package app.reseam.youtube.navbuttons

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.reseam.bundle.Logger
import app.reseam.bundle.identifier
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.core.dp
import java.lang.ref.WeakReference
import java.util.WeakHashMap

private val SEARCH_BUTTONS = setOf("SEARCH", "SEARCH_CAIRO", "SEARCH_BOLD")

/** Each search button YouTube built, with the menu item it is the action view of. */
private val searchButtons = WeakHashMap<View, MenuItem>()

/** The search button each toolbar hid for its bar, which the bar clicks. */
private val replacedButtons = WeakHashMap<ViewGroup, View>()

/** The bar's click target when Hide toolbar search button dropped the button before it attached. */
@Volatile private var latestSearchButton = WeakReference<View>(null)

@Export
fun addSearchBar(header: View?) {
    if (header !is ViewGroup) return
    if (header.findViewWithTag<View>(SearchBarView.TAG) != null) return
    val logo = header.findViewById<View>(header.context.identifier("youtube_logo", "id"))
    header.addView(
        SearchBarView(header.context, logo),
        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(36f), Gravity.END or Gravity.CENTER_VERTICAL)
            .apply { marginEnd = dp(12f) },
    )
    Logger.debug { "Wide search bar added, logo=${logo != null}" }
}

@Export
fun searchButtonIdentified(button: Enum<*>?, view: View?, menuItem: MenuItem?) {
    if (view == null || menuItem == null || button?.name !in SEARCH_BUTTONS) return
    latestSearchButton = WeakReference(view)
    if (searchButtons.put(view, menuItem) == null) view.addOnAttachStateChangeListener(AttachListener { replaceSearchButton(view) })
    replaceSearchButton(view)
}

/**
 * A toolbar showing the bar drops its search button's menu item, so the action menu gives its
 * width to the header. The menu presenter makes every action view visible again on each update,
 * so the view's own visibility cannot hide it.
 */
private fun replaceSearchButton(button: View) {
    val toolbar = toolbarOf(button) ?: return
    if (barIn(toolbar)?.isAttachedToWindow != true) return
    replacedButtons[toolbar] = button
    searchButtons[button]?.takeIf { it.isVisible }?.setVisible(false)
    Logger.debug { "Wide search bar replaced the search button" }
}

private fun toolbarOf(view: View): ViewGroup? {
    val toolbarId = view.context.identifier("toolbar", "id")
    var parent = view.parent
    while (parent is ViewGroup) {
        if (parent.id == toolbarId) return parent
        parent = parent.parent
    }
    return null
}

private fun barIn(toolbar: ViewGroup): View? = toolbar.findViewWithTag(SearchBarView.TAG)

private fun searchButtonsIn(toolbar: ViewGroup): List<View> = searchButtons.keys.filter { toolbarOf(it) === toolbar }

private class AttachListener(private val onAttach: () -> Unit) : View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(view: View) = onAttach()

    override fun onViewDetachedFromWindow(view: View) = Unit
}

private class SearchBarView(context: Context, private val logo: View?) : LinearLayout(context) {
    init {
        tag = TAG
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPaddingRelative(dp(12f), 0, dp(16f), 0)
        background = GradientDrawable().apply {
            cornerRadius = dp(18f).toFloat()
            setColor(themeColor("ytAdditiveBackground"))
        }
        val ripple = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
        foreground = ripple.getDrawable(0)
        ripple.recycle()
        val hint = context.getString(context.identifier("search_hint", "string"))
        contentDescription = hint
        isClickable = true
        isFocusable = true
        val color = themeColor("ytTextSecondary")
        addView(
            ImageView(context).apply {
                setImageResource(context.identifier("yt_outline_search_black_24", "drawable"))
                setColorFilter(color)
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LayoutParams(dp(20f), dp(20f)).apply { marginEnd = dp(12f) },
        )
        addView(
            TextView(context).apply {
                text = hint
                setTextColor(color)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f),
        )
        setOnClickListener {
            (toolbarOf(this)?.let { replacedButtons[it] ?: searchButtonsIn(it).firstOrNull() } ?: latestSearchButton.get())?.performClick()
        }
        addOnAttachStateChangeListener(AttachListener { toolbarOf(this)?.let(::searchButtonsIn)?.forEach(::replaceSearchButton) })
    }

    // The header lays the logo out first, so its measured width is known here.
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val logoEnd = logo?.takeIf { it.visibility != GONE }?.let { logo ->
            logo.measuredWidth + ((logo.layoutParams as? MarginLayoutParams)?.let { it.marginStart + it.marginEnd } ?: 0)
        } ?: 0
        val width = (MeasureSpec.getSize(widthMeasureSpec) - logoEnd - dp(12f)).coerceAtLeast(0)
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec)
    }

    private fun themeColor(attr: String): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(context.identifier(attr, "attr"), value, true)
        return value.data
    }

    companion object {
        const val TAG = "reseam_wide_search_bar"
    }
}
