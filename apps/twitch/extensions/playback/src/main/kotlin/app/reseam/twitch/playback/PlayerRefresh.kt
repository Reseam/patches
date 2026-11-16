// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PlayerRefresh")

package app.reseam.twitch.playback

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import app.reseam.runtime.Export
import app.reseam.twitch.settings.TwitchStrings
import java.lang.ref.WeakReference
import java.util.WeakHashMap

// The presenter keeps its player view alive, so the map holds it weakly too.
private val presenters = WeakHashMap<View, WeakReference<Any>>()
private var lastPlayerView: WeakReference<View>? = null

@Export
fun register(playerView: View?, presenter: Any?) {
    if (playerView == null || presenter == null) return
    presenters[playerView] = WeakReference(presenter)
    lastPlayerView = WeakReference(playerView)
}

/** Puts a refresh button with [icon] just before the native player's settings [gear]. */
@Export
fun addButton(gear: View?, icon: Int) {
    val parent = gear?.parent as? ViewGroup
    if (gear !is ImageView || parent == null) return
    val refresh = ImageView(gear.context).apply {
        setImageResource(icon)
        scaleType = gear.scaleType
        background = gear.background?.constantState?.newDrawable()?.mutate()
        imageTintList = gear.imageTintList
        setPadding(gear.paddingLeft, gear.paddingTop, gear.paddingRight, gear.paddingBottom)
        contentDescription = TwitchStrings.refreshStream
        setOnClickListener { view -> presenterFor(view)?.let(::reload) }
    }
    // The overlay's constraints point at the gear's id, so the row that holds both buttons takes it over.
    val row = LinearLayout(gear.context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        id = gear.id
    }
    val params = gear.layoutParams
    val index = parent.indexOfChild(gear)
    parent.removeViewAt(index)
    gear.id = View.generateViewId()
    row.addView(refresh, LinearLayout.LayoutParams(params.width, params.height))
    row.addView(gear, LinearLayout.LayoutParams(params.width, params.height))
    // Twitch hides and fades the gear itself, and the button follows it while the player is on screen.
    val gearFollower = ViewTreeObserver.OnPreDrawListener {
        refresh.visibility = gear.visibility
        refresh.alpha = gear.alpha
        true
    }
    refresh.addOnAttachStateChangeListener(
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = view.viewTreeObserver.addOnPreDrawListener(gearFollower)

            override fun onViewDetachedFromWindow(view: View) = view.viewTreeObserver.removeOnPreDrawListener(gearFollower)
        },
    )
    params.width = ViewGroup.LayoutParams.WRAP_CONTENT
    parent.addView(row, index, params)
}

/** Runs Twitch's Retry: a new access token and playlist for a live stream, the same position for a video. */
@Export
external fun reload(presenter: Any?)

// The controls and the player view sit side by side in the theatre, so the nearest ancestor that holds a player view picks it.
private fun presenterFor(control: View): Any? {
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
