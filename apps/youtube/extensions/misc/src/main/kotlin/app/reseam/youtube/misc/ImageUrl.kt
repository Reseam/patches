// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("ImageUrl")

package app.reseam.youtube.misc

import app.reseam.runtime.Export
import app.reseam.youtube.settings.AppearanceSettings

private const val REPLACEMENT_HOST = "https://yt4.ggpht.com"
private val staticImageHost =
    Regex("^https://(yt3|lh[3-6]|play-lh)\\.(ggpht|googleusercontent)\\.com(?=/|$|[?#])")

@Export
fun `override`(url: String?): String? =
    if (url == null || !AppearanceSettings.bypassImageRegionRestrictions.value) url
    else staticImageHost.replaceFirst(url, REPLACEMENT_HOST)
