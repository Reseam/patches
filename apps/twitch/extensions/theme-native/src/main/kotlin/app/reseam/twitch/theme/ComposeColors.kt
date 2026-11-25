// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ComposeColors")

package app.reseam.twitch.theme

import android.content.Context
import app.reseam.runtime.Export

/** Returns the app's color [id] as a Compose color, or [color] when there is no application yet. */
@Export
fun appColor(color: Long, id: Int): Long {
    val application = currentApplication() ?: return color
    // Compose keeps an sRGB color's ARGB bits above 32 bits that name the color space.
    return (application.getColor(id).toLong() and 0xFFFFFFFFL) shl 32
}

// A class initializer gets no context, so the app comes from the framework.
private fun currentApplication(): Context? =
    try {
        Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as? Context
    } catch (_: ReflectiveOperationException) {
        null
    }
