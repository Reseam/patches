// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SwipeControls")

package app.reseam.youtube.swipe

import android.app.Activity
import android.media.AudioManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import app.reseam.bundle.Logger
import app.reseam.bundle.mainThreadHandler
import app.reseam.runtime.Export
import app.reseam.runtime.TextValue
import app.reseam.youtube.player.PlayerType
import app.reseam.youtube.settings.PlayerControlsSettings
import java.lang.ref.WeakReference

private val volumeKeys = setOf(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
private var current = WeakReference<SwipeState>(null)
internal val swipeMain =
    mainThreadHandler().also { handler ->
        PlayerType.onChange.add { type -> handler.post { current.get()?.playerChanged(type) } }
    }

@Export
fun initialize(activity: Activity) {
    val previous = current.get()
    if (previous != null && previous.activity === activity) return
    val state = SwipeState(activity)
    // The view tree keeps the state alive for exactly the activity's lifetime.
    state.overlay.tag = state
    current = WeakReference(state)
    (state.activity.window.decorView as ViewGroup).addView(
        state.overlay,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ),
    )
}

@Export
fun touch(activity: Activity, event: MotionEvent?): Boolean {
    val state = current.get() ?: return false
    if (state.activity !== activity || state.forwarding || event == null) return false
    return try {
        state.touch(event)
    } catch (ex: RuntimeException) {
        state.abortGesture()
        Logger.error(ex) { "Swipe controls" }
        false
    }
}

@Export
fun key(activity: Activity, event: KeyEvent?): Boolean {
    val state = current.get() ?: return false
    if (
        state.activity !== activity ||
            event == null ||
            !isSwipeFullscreen ||
            !PlayerControlsSettings.swipeVolume.value
    )
        return false
    val key = event.keyCode
    if (key !in volumeKeys) return false
    if (event.action == KeyEvent.ACTION_DOWN) {
        val volume =
            state.audio.getStreamVolume(AudioManager.STREAM_MUSIC) +
                if (key == KeyEvent.KEYCODE_VOLUME_UP) 1 else -1
        state.setVolume(volume)
    }
    return true
}

internal val isSwipeFullscreen: Boolean
    get() = PlayerType.current == PlayerType.WATCH_WHILE_FULLSCREEN

internal fun swipeNumber(setting: TextValue, fallback: Float, min: Float, max: Float): Float =
    setting.value.toFloatOrNull()?.takeIf { it.isFinite() }?.coerceIn(min, max) ?: fallback
