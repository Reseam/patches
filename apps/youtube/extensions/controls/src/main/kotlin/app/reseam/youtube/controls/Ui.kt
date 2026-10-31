// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("Ui")

package app.reseam.youtube.controls

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import app.reseam.bundle.identifier
import app.reseam.bundle.isNightMode
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.core.dp

fun roundedCorners(dp: Float): FloatArray {
    val radius = dp(dp).toFloat()
    return FloatArray(8) { radius }
}

fun portraitWidth(percent: Int): Int =
    Resources.getSystem().displayMetrics.let {
        minOf(it.widthPixels, it.heightPixels) * percent / 100
    }

/** YouTube's theme choice can differ from the configuration's night mode. */
fun isDarkMode(context: Context): Boolean {
    val background = TypedValue()
    val hasBackgroundColor =
        context.theme.resolveAttribute(android.R.attr.colorBackground, background, true) &&
            background.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT
    return if (hasBackgroundColor) Color.luminance(background.data) < 0.5f
    else context.resources.isNightMode()
}

fun dialogBackgroundColor(context: Context): Int {
    val surface = themeColor(context, "yt_sys_color_baseline_menu_background")
        ?: if (isDarkMode(context)) appColor(context, "yt_black1", Color.BLACK) else appColor(context, "yt_white1", Color.WHITE)
    // Lift pure black so the dialog remains visible on the AMOLED theme.
    return if (surface == Color.BLACK) 0xFF080808.toInt() else surface
}

fun foregroundColor(context: Context): Int =
    if (isDarkMode(context)) {
        appColor(context, "yt_white1", Color.WHITE)
    } else {
        appColor(context, "yt_black1", Color.BLACK)
    }

fun adjustBrightness(context: Context, color: Int, lightFactor: Float, darkFactor: Float): Int {
    val factor = if (isDarkMode(context)) darkFactor else lightFactor
    fun adjust(channel: Int): Int {
        val adjusted =
            if (factor > 1.0f) {
                Math.round(channel + (255 - channel) * (1.0f - 1.0f / factor))
            } else {
                Math.round(channel * factor)
            }
        return adjusted.coerceIn(0, 255)
    }
    return Color.argb(
        Color.alpha(color),
        adjust(Color.red(color)),
        adjust(Color.green(color)),
        adjust(Color.blue(color)),
    )
}

fun fadeInDuration(): Int {
    val context = YouTubeContext.applicationContext
    return context.resources.getInteger(context.identifier("fade_duration_fast", "integer"))
}

private fun themeColor(context: Context, attr: String): Int? {
    val id = context.identifier(attr, "attr")
    val value = TypedValue()
    val resolved = id != 0 && context.theme.resolveAttribute(id, value, true)
    return value.data.takeIf { resolved && value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT }
}

private fun appColor(context: Context, name: String, fallback: Int): Int {
    val id = context.identifier(name, "color")
    return if (id == 0) fallback else context.getColor(id)
}
