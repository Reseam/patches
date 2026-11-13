// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.player

import android.view.View
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import java.util.Collections
import java.util.WeakHashMap

object ShortsPlayer {
    @Volatile
    var isOpen: Boolean = false
        private set

    /** A Short is playing: its player is open and no regular video is over it. */
    val isActive: Boolean
        get() = isOpen && PlayerType.current.isNoneOrHidden

    private val observed = Collections.newSetFromMap(WeakHashMap<View, Boolean>())
    private val attached = Collections.newSetFromMap(WeakHashMap<View, Boolean>())

    @Export
    @JvmStatic
    fun attach(shortsPlayer: View?) {
        if (shortsPlayer == null || !observed.add(shortsPlayer)) return
        if (shortsPlayer.isAttachedToWindow) {
            attached.add(shortsPlayer)
            updateOpen(true)
        }
        shortsPlayer.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    attached.add(view)
                    updateOpen(true)
                }

                override fun onViewDetachedFromWindow(view: View) {
                    attached.remove(view)
                    updateOpen(attached.isNotEmpty())
                }
            }
        )
    }

    private fun updateOpen(value: Boolean) {
        if (isOpen == value) return
        isOpen = value
        Logger.debug { "Shorts player open: $value" }
    }
}
