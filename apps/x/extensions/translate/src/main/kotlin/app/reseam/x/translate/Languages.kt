// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

import java.util.Locale

private val TRADITIONAL_CHINESE_REGIONS = setOf("TW", "HK", "MO")

/** The translation engine's code for a language, from a post's language tag or the device locale. */
internal fun engineLanguage(locale: Locale): String =
    when (val language = locale.language) {
        "zh" -> if (locale.script == "Hant" || locale.country in TRADITIONAL_CHINESE_REGIONS) "zh-Hant" else "zh-Hans"
        "in" -> "id"
        "iw" -> "he"
        else -> language
    }
