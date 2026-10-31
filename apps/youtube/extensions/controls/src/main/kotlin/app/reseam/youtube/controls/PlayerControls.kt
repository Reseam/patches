// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.controls

import android.view.View
import android.view.ViewTreeObserver
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.abs

object PlayerControls {
    private val buttons = Collections.newSetFromMap(WeakHashMap<PlayerControlButton, Boolean>())
    private val visibilityListeners = CopyOnWriteArraySet<() -> Unit>()
    private val visibilityBySuffix =
        mapOf("SHOWN" to true, "WILL_SHOW" to true, "HIDDEN" to false, "WILL_HIDE" to false)

    @Volatile
    var isVisible: Boolean = false
        private set

    fun addVisibilityListener(listener: () -> Unit) {
        visibilityListeners.add(listener)
    }

    fun register(button: PlayerControlButton) {
        buttons.add(button)
        button.setVisibility(isVisible)
    }

    @Export
    @JvmStatic
    fun setVisibility(visible: Boolean, animated: Boolean) {
        val changed = isVisible != visible
        isVisible = visible
        // Weak entries can disappear while iterating; the array snapshot tolerates that.
        buttons.toTypedArray().forEach { it.setVisibility(visible) }
        Logger.debug { "Player controls visibility: ${if (visible) "shown" else "hidden"}" }
        if (changed) visibilityListeners.forEach { it() }
    }

    @Export @JvmStatic fun setVisibilityImmediate(visible: Boolean) = setVisibility(visible, false)

    @Export
    @JvmStatic
    fun setPlayerControlsVisibility(state: Enum<*>?) {
        val name = state?.name ?: return
        val visible =
            visibilityBySuffix.entries.firstOrNull { name.endsWith(it.key) }?.value ?: return
        setVisibility(visible, true)
    }

    @Export
    @JvmStatic
    fun setFullscreenCloseButton(button: View) {
        button.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                private var lastVisibility = button.visibility
                private val position = IntArray(2)
                private val referencePosition = IntArray(2)
                private var alignmentOffset = 0f
                private var appliedTranslation = 0f

                override fun onGlobalLayout() {
                    // A translation replaced by YouTube no longer includes our previous offset.
                    if (button.translationY != appliedTranslation) alignmentOffset = 0f
                    alignmentOffset =
                        alignFullscreenButton(button, position, referencePosition, alignmentOffset)
                    appliedTranslation = button.translationY
                    if (button.visibility == lastVisibility) return
                    lastVisibility = button.visibility
                    setVisibility(lastVisibility == View.VISIBLE, false)
                }
            }
        )
    }

    private fun alignFullscreenButton(
        button: View,
        position: IntArray,
        referencePosition: IntArray,
        alignmentOffset: Float,
    ): Float {
        if (!button.isShown || button.height == 0) return alignmentOffset
        button.getLocationInWindow(position)
        val center = button.contentCenterY(position[1])
        val nearest =
            buttons
                .mapNotNull { it.alignmentView }
                .filter { it.isShown && it.height != 0 && it.rootView === button.rootView }
                .minByOrNull {
                    it.getLocationInWindow(referencePosition)
                    abs(referencePosition[0] - position[0])
                }
        if (nearest == null) {
            if (alignmentOffset != 0f) button.translationY -= alignmentOffset
            return 0f
        }
        nearest.getLocationInWindow(referencePosition)
        val target = nearest.contentCenterY(referencePosition[1])
        val delta = target - center
        if (abs(delta) >= 1) {
            button.translationY += delta
            return alignmentOffset + delta
        }
        return alignmentOffset
    }
}

private fun View.contentCenterY(top: Int): Float =
    top + paddingTop + (height - paddingTop - paddingBottom) / 2f
