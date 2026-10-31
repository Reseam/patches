// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("Miniplayer")

package app.reseam.youtube.miniplayer

import android.view.View
import android.view.ViewGroup
import app.reseam.bundle.dpToPx
import app.reseam.runtime.Export
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.settings.PlayerSettings

private const val DEFAULT_WIDTH_DIP = 192
private const val SCREEN_MARGIN_DIP = 15

@Export
fun width(original: Int): Int {
    val requested = PlayerSettings.miniplayerWidthDip.value.toIntOrNull() ?: return original
    if (requested == DEFAULT_WIDTH_DIP) return original
    val resources = YouTubeContext.applicationContext.resources
    val metrics = resources.displayMetrics
    val maximum =
        ((metrics.widthPixels / metrics.density).toInt() - SCREEN_MARGIN_DIP).coerceAtLeast(
            DEFAULT_WIDTH_DIP
        )
    return resources.dpToPx(requested.coerceIn(DEFAULT_WIDTH_DIP, maximum))
}

@Export
fun hideOverlayButton(view: View?) {
    if (PlayerSettings.hideMiniplayerOverlayButtons.value) view?.hide()
}

@Export
fun hideSubtext(view: View?) {
    if (PlayerSettings.hideMiniplayerSubtext.value) view?.hide()
}

private fun View.hide() {
    val parent = parent as? ViewGroup
    if (parent != null) parent.removeView(this) else visibility = View.GONE
}
