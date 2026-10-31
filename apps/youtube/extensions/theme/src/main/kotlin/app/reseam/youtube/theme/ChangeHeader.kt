// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ChangeHeader")

package app.reseam.youtube.theme

import app.reseam.bundle.Logger
import app.reseam.bundle.identifier
import app.reseam.runtime.Export
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.AppearanceSettings

@Export
fun headerAttribute(original: Int): Int {
    val selected = AppearanceSettings.changeHeaderLogo.value
    val name =
        when (selected) {
            "premium" -> "ytPremiumWordmarkHeader"
            "reseam" -> "reseam_header"
            else -> return original
        }
    val context = YouTubeContext.applicationContext
    val id = context.identifier(name, "attr")
    Logger.debug { "Header logo: $selected -> $name" }
    return if (id == 0) original else id
}
