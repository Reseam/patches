// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("AdvancedVideoQualityMenu")

package app.reseam.youtube.quality

import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.settings.VideoSettings

@Volatile private var quickMenuVisible = false

fun markQuickMenuVisible() {
    quickMenuVisible = true
}

@Export
fun onFlyoutMenuCreate(view: View) {
    if (!VideoSettings.advancedVideoQualityMenu.value) return
    val list = view as? ViewGroup ?: return
    list.viewTreeObserver.addOnDrawListener { openAdvancedMenu(list) }
}

private fun openAdvancedMenu(list: ViewGroup) {
    if (!quickMenuVisible || list.childCount == 0) return
    quickMenuVisible = false
    val first = list.getChildAt(0) as? ViewGroup ?: return
    if (first.childCount < 4) return
    val advanced = first.getChildAt(3)
    advanced.isSoundEffectsEnabled = false
    clickWhenSettled(list, advanced)
}

// Switching content while the sheet slides in can leave it empty; wait for two stable frames.
private fun clickWhenSettled(list: View, advanced: View) {
    Choreographer.getInstance()
        .postFrameCallback(
            object : Choreographer.FrameCallback {
                private val location = IntArray(2)
                private var lastY = Int.MIN_VALUE
                private var stableFrames = 0

                override fun doFrame(frameTimeNanos: Long) {
                    if (!advanced.isAttachedToWindow) return
                    list.getLocationOnScreen(location)
                    stableFrames = if (location[1] == lastY) stableFrames + 1 else 0
                    lastY = location[1]
                    if (stableFrames < 2) {
                        Choreographer.getInstance().postFrameCallback(this)
                    } else advanced.performClick()
                }
            }
        )
}

@Export
fun addVideoQualityListMenuListener(view: View) {
    if (!VideoSettings.advancedVideoQualityMenu.value) return
    val list = view as? ListView ?: return
    list.setOnHierarchyChangeListener(
        object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) {
                try {
                    if (list.indexOfChild(child) != 4) return
                    list.isSoundEffectsEnabled = false
                    if (!list.performItemClick(child, 4, list.getItemIdAtPosition(4))) return
                    parent.visibility = View.GONE
                } catch (exception: Exception) {
                    Logger.error(exception) { "Advanced Shorts quality menu failure" }
                }
            }

            override fun onChildViewRemoved(parent: View, child: View) {}
        }
    )
}

@Export
fun forceAdvancedVideoQualityMenuCreation(original: Boolean): Boolean =
    VideoSettings.advancedVideoQualityMenu.value || original
