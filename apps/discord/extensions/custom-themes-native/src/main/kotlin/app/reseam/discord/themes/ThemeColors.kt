// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ThemeColors")

package app.reseam.discord.themes

import android.content.Context
import app.reseam.runtime.Export
import java.io.IOException
import org.json.JSONException
import org.json.JSONObject

// Kept on disk so views drawn before JavaScript starts use the same palette.
@Volatile private var palette = Palette("", emptyMap())

@Export
@Throws(IOException::class, JSONException::class)
fun init(context: Context) {
    palette = parse(context.themeFile("native-colors.json").readAtomically())
}

@Export
fun color(theme: String, token: String, color: Int): Int {
    val current = palette
    return if (theme == current.base) current.colors[token] ?: color else color
}

@Throws(IOException::class, JSONException::class)
internal fun applyNativePalette(context: Context, json: String) {
    val next = parse(json)
    context.themeFile("native-colors.json").writeAtomically(json)
    palette = next
}

private data class Palette(val base: String, val colors: Map<String, Int>)

private fun parse(json: String): Palette {
    if (json.isEmpty()) return Palette("", emptyMap())
    val root = JSONObject(json)
    val entries = root.getJSONObject("colors")
    val colors = entries.keys().asSequence().associateWith { entries.getLong(it).toInt() }
    return Palette(root.getString("base"), colors)
}
