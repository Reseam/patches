// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ThreadLines")

package app.reseam.reddit.comments

import android.graphics.Color
import app.reseam.reddit.settings.RedditSettings
import app.reseam.runtime.Export

private val colors = RedditSettings.threadLineColors.let { setting ->
    runCatching { parse(setting.value) }.getOrElse { parse(setting.default) }
}

private fun parse(list: String) = list.split(',').map { Color.parseColor(it.trim()) }

/** Compose packs an sRGB color as its ARGB in the high 32 bits. */
@Export fun color(depth: Int): Long = colors[(depth - 1) % colors.size].toLong() shl 32

/** The colors carry their own opacity; a collapsed thread keeps Reddit's fade. */
@Export fun alpha(alpha: Float, faded: Boolean): Float = if (faded) alpha else 1f
