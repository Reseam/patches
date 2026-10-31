// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("MusicTheme")

package app.reseam.youtubemusic.layout

import app.reseam.runtime.Export

private const val PURE_BLACK = -0x1000000

// Replaced at patch time with the chosen background color.
@Export private fun background(): Int = PURE_BLACK

@Export fun paintColor(original: Int): Int = if (original == PURE_BLACK) background() else original
