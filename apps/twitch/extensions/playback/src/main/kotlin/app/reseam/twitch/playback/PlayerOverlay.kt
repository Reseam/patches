// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.playback

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import java.lang.ref.WeakReference
import java.util.WeakHashMap

// The presenter keeps its player view alive.
private val presenters = WeakHashMap<View, WeakReference<Any>>()
private var lastPlayerView: WeakReference<View>? = null

// The first button moves the gear into a row with this tag, and later buttons join that row.
private val buttonRow = Any()

internal fun registerPlayer(playerView: View?, presenter: Any?) {
    if (playerView == null || presenter == null) return
    presenters[playerView] = WeakReference(presenter)
    lastPlayerView = WeakReference(playerView)
}

// The controls sit beside the player view, so the presenter comes from the nearest ancestor that holds one.
internal fun presenterFor(control: View): Any? {
    var ancestor: View? = control
    while (ancestor != null) {
        val owner = ancestor
        presenters.entries.firstOrNull { it.key.isInside(owner) }?.value?.get()?.let { return it }
        ancestor = ancestor.parent as? View
    }
    return lastPlayerView?.get()?.takeIf { it.isAttachedToWindow }?.let { presenters[it]?.get() }
}

private fun View.isInside(ancestor: View): Boolean {
    var view: View? = this
    while (view != null) {
        if (view === ancestor) return true
        view = view.parent as? View
    }
    return false
}

internal fun gearStyledButton(gear: ImageView, icon: Int, label: String) =
    ImageView(gear.context).apply {
        setImageResource(icon)
        scaleType = gear.scaleType
        background = gear.background?.constantState?.newDrawable()?.mutate()
        imageTintList = gear.imageTintList
        setPadding(gear.paddingLeft, gear.paddingTop, gear.paddingRight, gear.paddingBottom)
        contentDescription = label
    }

internal fun addBeforeGear(gear: ImageView, button: ImageView, shown: () -> Boolean = { true }) {
    val parent = gear.parent as? ViewGroup ?: return
    if (parent.tag === buttonRow) {
        parent.addView(button, parent.indexOfChild(gear), LinearLayout.LayoutParams(gear.layoutParams.width, gear.layoutParams.height))
    } else {
        // The overlay's constraints point at the gear's id.
        val row = LinearLayout(gear.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            id = gear.id
            tag = buttonRow
        }
        val params = gear.layoutParams
        val index = parent.indexOfChild(gear)
        parent.removeViewAt(index)
        gear.id = View.generateViewId()
        row.addView(button, LinearLayout.LayoutParams(params.width, params.height))
        row.addView(gear, LinearLayout.LayoutParams(params.width, params.height))
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        parent.addView(row, index, params)
    }
    // Twitch hides and fades the gear itself.
    button.beforeEachDraw {
        button.visibility = if (shown()) gear.visibility else View.GONE
        button.alpha = gear.alpha
        true
    }
}

/** Runs [listener] before each frame while this view is attached; returning false skips that frame. */
internal fun View.beforeEachDraw(listener: ViewTreeObserver.OnPreDrawListener) {
    addOnAttachStateChangeListener(
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = view.viewTreeObserver.addOnPreDrawListener(listener)

            override fun onViewDetachedFromWindow(view: View) = view.viewTreeObserver.removeOnPreDrawListener(listener)
        },
    )
}
