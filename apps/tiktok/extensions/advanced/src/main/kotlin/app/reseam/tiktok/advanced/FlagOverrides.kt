// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FlagOverrides")

package app.reseam.tiktok.advanced

import app.reseam.runtime.Export
import app.reseam.runtime.Reseam
import app.reseam.tiktok.settings.AdvancedSettings

// TikTok reads flags before the runtime starts; those reads see no overrides and parse nothing.
private var overrides: Map<String, String>? = null

private fun overrides(): Map<String, String> =
    overrides
        ?: if (!Reseam.isStarted) emptyMap()
        else
            AdvancedSettings.flagOverrides.value
                .lines()
                .filter { '=' in it }
                .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
                .also { overrides = it }

@Export
fun booleanOverride(key: String?, original: Boolean): Boolean =
    override(key, original) { it.toBooleanStrictOrNull() }

@Export
fun intOverride(key: String?, original: Int): Int = override(key, original) { it.toIntOrNull() }

@Export
fun longOverride(key: String?, original: Long): Long = override(key, original) { it.toLongOrNull() }

@Export
fun stringOverride(key: String?, original: String?): String? = override(key, original) { it }

private inline fun <T> override(key: String?, original: T, parse: (String) -> T?): T =
    key?.let(overrides()::get)?.let(parse) ?: original
