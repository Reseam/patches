// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("XTheme")

package app.reseam.x.theme

import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings

/** The current theme's primary color, the accent; Twitter's brand color with X's blue accent. */
@Export external fun brandColor(composer: Any?): Long

/** A Compose defaults mask with [parameter]'s bit cleared, so the value passed for it is used. */
@Export fun withoutDefault(mask: Int, parameter: Int): Int = mask and parameter.inv()

/** Whether night mode uses Dim instead of Lights out. */
@Export fun dim(): Boolean = XSettings.darkTheme.value == "dim"

// The grays of X's newer palette in Lights out, and the Dim shades that take their place; most are
// the ones X's own Dim palette uses.
private val dimShades =
    mapOf(
        0xff030303 to 0xff15202b,
        0xff141414 to 0xff1e2732,
        0xff262626 to 0xff2c3640,
        0xff4d4d4d to 0xff3d5466,
        0xff37434d to 0xff38444d,
    ).entries.associate { (dark, dim) -> dark.toInt() to dim.toInt() }

/** [color], a Compose sRGB color value, as its Dim shade when Dim is on and it is a Lights out gray. */
@Export
fun dimmed(color: Long): Long {
    if (!dim()) return color
    val shade = dimShades[(color ushr 32).toInt()] ?: return color
    return composeColor(shade)
}

/** Whether [accent] replaces the color picked in X's display settings. */
@Export fun overridesAccent(): Boolean = accentArgb() != null

/** The accent as a Compose sRGB color value. Only meaningful when [overridesAccent]. */
@Export fun accent(): Long = composeColor(accentArgb() ?: 0)

private fun composeColor(argb: Int): Long = argb.toLong().and(0xffffffffL) shl 32

private fun accentArgb(): Int? =
    when (XSettings.accentColor.value) {
        // System palettes are overlaid onto the framework's own resources, so no context is needed.
        "material_you" ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Resources.getSystem().getColor(android.R.color.system_accent1_400, null)
            else null
        "custom" -> runCatching { Color.parseColor(XSettings.customAccentColor.value.trim()) }.getOrNull()
        else -> null
    }
