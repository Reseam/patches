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

// The presenter keeps its player view alive, so the map holds it weakly too.
private val presenters = WeakHashMap<View, WeakReference<Any>>()
private var lastPlayerView: WeakReference<View>? = null

// The first button moves the gear into a row with this tag, and later buttons join that row.
private val buttonRow = Any()

internal fun registerPlayer(playerView: View?, presenter: Any?) {
    if (playerView == null || presenter == null) return
    presenters[playerView] = WeakReference(presenter)
    lastPlayerView = WeakReference(playerView)
}

// The controls and the player view sit side by side in the theatre, so the presenter comes from the nearest ancestor that holds a player view.
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

/** Returns a button with [icon] that looks like the native player's settings [gear]. */
internal fun gearStyledButton(gear: ImageView, icon: Int, label: String) =
    ImageView(gear.context).apply {
        setImageResource(icon)
        scaleType = gear.scaleType
        background = gear.background?.constantState?.newDrawable()?.mutate()
        imageTintList = gear.imageTintList
        setPadding(gear.paddingLeft, gear.paddingTop, gear.paddingRight, gear.paddingBottom)
        contentDescription = label
    }

/** Puts [button] just before the settings [gear], and shows it with the gear while [shown] returns true. */
internal fun addBeforeGear(gear: ImageView, button: ImageView, shown: () -> Boolean = { true }) {
    val parent = gear.parent as? ViewGroup ?: return
    if (parent.tag === buttonRow) {
        parent.addView(button, parent.indexOfChild(gear), LinearLayout.LayoutParams(gear.layoutParams.width, gear.layoutParams.height))
    } else {
        // The overlay's constraints point at the gear's id, so the row that holds the buttons gets that id.
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
    // Twitch hides and fades the gear itself, so each frame copies the gear's visibility and alpha to the button while the player is on screen.
    val gearFollower = ViewTreeObserver.OnPreDrawListener {
        button.visibility = if (shown()) gear.visibility else View.GONE
        button.alpha = gear.alpha
        true
    }
    button.addOnAttachStateChangeListener(
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = view.viewTreeObserver.addOnPreDrawListener(gearFollower)

            override fun onViewDetachedFromWindow(view: View) = view.viewTreeObserver.removeOnPreDrawListener(gearFollower)
        },
    )
}
