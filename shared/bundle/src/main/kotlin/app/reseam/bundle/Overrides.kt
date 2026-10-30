// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

/**
 * `key=value` entries separated by commas or line breaks, as users write flag overrides. A value in
 * brackets keeps its commas.
 */
fun parseOverrides(text: String): Map<String, String> = buildMap {
    var start = 0
    var depth = 0
    fun entry(end: Int) {
        val entry = text.substring(start, end)
        if ('=' in entry) put(entry.substringBefore('=').trim(), entry.substringAfter('=').trim())
        start = end + 1
    }
    text.forEachIndexed { i, ch ->
        when (ch) {
            '[' -> depth++
            ']' -> depth--
            ',', '\n' -> if (depth == 0) entry(i)
        }
    }
    entry(text.length)
}
