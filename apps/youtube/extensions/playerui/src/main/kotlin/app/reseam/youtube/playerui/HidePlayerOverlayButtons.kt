// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("HidePlayerOverlayButtons")

package app.reseam.youtube.playerui

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageView
import android.widget.RelativeLayout
import app.reseam.bundle.Logger
import app.reseam.bundle.identifier
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.PlayerOverlaySettings

@Export(name = "castVisibility")
fun getCastButtonOverrideV2(original: Int): Int {
    val hide = PlayerOverlaySettings.hideCastButton.value
    Logger.debug { "HidePlayerOverlayButtons.castVisibility: $hide" }
    return if (hide) View.GONE else original
}

@Export(name = "castEnabled")
fun getCastButtonOverrideV2(original: Boolean): Boolean {
    val hide = PlayerOverlaySettings.hideCastButton.value
    Logger.debug { "HidePlayerOverlayButtons.castEnabled: $hide" }
    return !hide && original
}

@Export
fun hideCaptionsButton(button: ImageView?) {
    val hide = PlayerOverlaySettings.hideCaptionsButton.value
    Logger.debug { "HidePlayerOverlayButtons.captions: $hide" }
    if (hide && button != null) button.visibility = View.GONE
}

@Export
fun hideCollapseButton(button: ImageView?) {
    val hide = PlayerOverlaySettings.hideCollapseButton.value
    Logger.debug { "HidePlayerOverlayButtons.collapse: $hide" }
    if (!hide || button == null) return
    button.setImageResource(android.R.color.transparent)
    button.imageAlpha = 0
    button.isEnabled = false
    (button.layoutParams as? RelativeLayout.LayoutParams)?.apply {
        width = 0
        height = 0
    }
}

@Export
fun setTitleAnchorStartMargin(view: View?) {
    val hide = PlayerOverlaySettings.hideCollapseButton.value
    Logger.debug { "HidePlayerOverlayButtons.titleAnchor: $hide" }
    if (!hide || view == null) return
    (view.layoutParams as? RelativeLayout.LayoutParams)?.marginStart = 0
}

@Export
fun hidePreviousNextButtons(parent: View?) {
    val hide = PlayerOverlaySettings.hidePlayerPreviousNextButtons.value
    Logger.debug { "HidePlayerOverlayButtons.previousNext: $hide" }
    if (!hide || parent == null) return
    parent.post {
        listOf("player_control_previous_button_touch_area", "player_control_next_button_touch_area")
            .map { parent.context.identifier(it, "id") }
            .forEach { remove(parent, it) }
    }
}

private fun remove(parent: View, id: Int) {
    if (id == 0) return
    val view = parent.findViewById<View>(id) ?: return
    val group = view.parent as? ViewGroup
    if (group != null) group.removeView(view) else view.visibility = View.GONE
}

@Export
fun hideFullscreenButton(view: View?): View? {
    val hide = PlayerOverlaySettings.hideFullscreenButton.value
    Logger.debug { "HidePlayerOverlayButtons.fullscreen: $hide" }
    if (hide && view != null) {
        view.visibility = View.GONE
        return null
    }
    return view
}

@Export
fun inflatePlayerControlButtons(stub: ViewStub): View =
    stub.inflate().also { hidePlayerControlButtonsBackground(it) }

@Export
fun hidePlayerControlButtonsBackground(root: View?) {
    if (root != null && PlayerOverlaySettings.hidePlayerControlButtonsBackground.value)
        clearBackgrounds(root)
}

private fun clearBackgrounds(view: View?) {
    if (view is ImageView) view.setBackgroundColor(Color.TRANSPARENT)
    if (view !is ViewGroup) return
    (0 until view.childCount).forEach { clearBackgrounds(view.getChildAt(it)) }
}
