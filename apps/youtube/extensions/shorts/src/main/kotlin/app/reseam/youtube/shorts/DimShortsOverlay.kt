// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("DimShortsOverlay")

package app.reseam.youtube.shorts

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import app.reseam.bundle.identifier
import app.reseam.runtime.Export
import app.reseam.youtube.core.findActivity
import app.reseam.youtube.player.ShortsPlayer
import app.reseam.youtube.settings.ShortsSettings
import java.util.WeakHashMap
import kotlin.math.roundToInt

private var reelTimeBarId = 0
private val dimmedViews = WeakHashMap<View, Unit>()

@Export
fun dimShortsPlayerOverlay(shortsOverlay: View?) {
    shortsOverlay?.drawWhileAttached { view ->
        val target = view.parent as? View ?: view
        val window = target.context.findActivity()?.window
        if (reelTimeBarId == 0) reelTimeBarId = view.context.identifier("reel_time_bar", "id")
        var timeBarHooked = false
        var hiddenWindow: Window? = null
        DrawSession(
            target,
            {
                target.dim(opacity())
                if (window != null && !timeBarHooked && reelTimeBarId != 0) {
                    window.decorView.findViewById<View>(reelTimeBarId)?.let {
                        addShortsAwareDimListener(it)
                        timeBarHooked = true
                    }
                }
                val immersive = ShortsSettings.dimShortsOverlayImmersiveMode.value
                if (window != null && immersive != (hiddenWindow != null)) {
                    hiddenWindow = window.takeIf { immersive }
                    setStatusBarHidden(window, immersive)
                }
            },
            { hiddenWindow?.let { setStatusBarHidden(it, false) } },
        )
    }
}

@Export
fun dimShortsToolbarButton(button: Enum<*>?, view: View?) {
    view?.let(::addShortsAwareDimListener)
}

// A black scrim dims the player as its alpha would over the black Shorts background, and unlike alpha it does
// not turn the video surface black before Android 10.
private fun View.dim(opacity: Float) {
    val alpha = ((1 - opacity) * 255).roundToInt()
    if ((foreground as? ColorDrawable)?.alpha == alpha) return
    foreground = ColorDrawable(Color.BLACK).also { it.alpha = alpha }
}

private fun opacity(): Float =
    (ShortsSettings.dimShortsOverlayOpacity.value.toIntOrNull()?.coerceIn(0, 100) ?: 100) / 100f

private fun setStatusBarHidden(window: Window, hide: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val controller = window.insetsController ?: return
        if (hide) {
            controller.hide(WindowInsets.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsets.Type.statusBars())
        }
        return
    }
    val decor = window.decorView
    val mask = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    decor.systemUiVisibility =
        if (hide) decor.systemUiVisibility or mask else decor.systemUiVisibility and mask.inv()
}

private fun addShortsAwareDimListener(view: View) {
    if (dimmedViews.put(view, Unit) != null) return
    view.drawWhileAttached { attached ->
        DrawSession(attached, { attached.alpha = if (ShortsPlayer.isOpen) opacity() else 1f })
    }
}

private class DrawSession(target: View, draw: () -> Unit, private val finish: () -> Unit = {}) {
    private val observer = target.viewTreeObserver
    private val listener = ViewTreeObserver.OnPreDrawListener {
        draw()
        true
    }

    init {
        observer.addOnPreDrawListener(listener)
    }

    fun close() {
        if (observer.isAlive) observer.removeOnPreDrawListener(listener)
        finish()
    }
}

private fun View.drawWhileAttached(start: (View) -> DrawSession) {
    val attachment =
        object : View.OnAttachStateChangeListener {
            private var session: DrawSession? = null

            override fun onViewAttachedToWindow(view: View) {
                session = start(view)
            }

            override fun onViewDetachedFromWindow(view: View) {
                session?.close()
                session = null
            }
        }
    addOnAttachStateChangeListener(attachment)
    if (isAttachedToWindow) attachment.onViewAttachedToWindow(this)
}
