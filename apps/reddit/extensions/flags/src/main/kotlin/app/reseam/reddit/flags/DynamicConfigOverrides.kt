// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("DynamicConfigOverrides")

package app.reseam.reddit.flags

import app.reseam.runtime.Export
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

private val parsed = ConcurrentHashMap<String, Map<String, String>>()
private val separator = Pattern.compile(";")

@Export fun intValue(name: String, overrides: String): Int? = stringValue(name, overrides)?.toInt()

// Overrides as "name=value;name=value", written in by the patch that owns the toggle.
@Export
fun stringValue(name: String, overrides: String): String? =
    parsed.computeIfAbsent(overrides, ::parse)[name]

private fun parse(overrides: String): Map<String, String> =
    separator.split(overrides).associate { entry ->
        val equalsIndex = entry.indexOf('=')
        entry.substring(0, equalsIndex) to entry.substring(equalsIndex + 1)
    }
