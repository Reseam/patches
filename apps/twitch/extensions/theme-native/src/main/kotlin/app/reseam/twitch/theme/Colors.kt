// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.theme

import android.content.Context
import app.reseam.bundle.identifier

/** Returns the app's color [name] as `#rrggbb`, or `#rrggbbaa` when it is translucent, or null when the app has no such color. */
internal fun Context.colorHex(name: String): String? {
    val id = identifier(name, "color")
    if (id == 0) return null
    val color = getColor(id)
    val rgb = "#" + (color and 0xFFFFFF).toString(16).padStart(6, '0')
    val alpha = color ushr 24
    return if (alpha == 0xFF) rgb else rgb + alpha.toString(16).padStart(2, '0')
}
