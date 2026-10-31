// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("CustomPlayerOverlayOpacity")

package app.reseam.youtube.playerui

import android.widget.ImageView
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.PlayerOverlaySettings

@Export
fun applyOpacity(overlay: ImageView?) {
    if (overlay == null) return
    val opacity = parseOpacity(PlayerOverlaySettings.playerOverlayOpacity.value)
    Logger.debug { "CustomPlayerOverlayOpacity: $opacity" }
    overlay.imageAlpha = opacity * 255 / 100
}

private fun parseOpacity(value: String): Int {
    val opacity = value.trim().toIntOrNull()
    if (opacity != null && opacity in 0..100) return opacity
    Logger.debug { "CustomPlayerOverlayOpacity: invalid value, using 100: $value" }
    return 100
}
