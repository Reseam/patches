// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.litho

import app.reseam.youtube.core.Proto
import app.reseam.youtube.core.bytes
import app.reseam.youtube.core.string
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import java.util.WeakHashMap

private const val THEME_PREFIX = "theme|"
private val themeMarker = ByteArrayFilterGroup(null, THEME_PREFIX)
// Identity keys; a null value marks a buffer without theme resources.
private val stripped = WeakHashMap<ByteArray, ByteArray?>()

/**
 * Templates embed their theme's resources (fonts, colors and every icon name the theme defines), so
 * a byte search would find any icon in any element built from them. Filters see elements without
 * them.
 */
internal fun withoutThemeResources(buffer: ByteArray): ByteArray {
    if (!themeMarker.check(buffer).isFiltered) return buffer
    synchronized(stripped) { if (buffer in stripped) return stripped[buffer] ?: buffer }
    val result = runCatching { removeThemeResources(buffer) }.getOrDefault(buffer)
    synchronized(stripped) { stripped[buffer] = result.takeIf { it !== buffer } }
    return result
}

private fun removeThemeResources(message: ByteArray): ByteArray =
    Proto.rewriteLength(message) { _, value ->
        when {
            !themeMarker.check(value).isFiltered -> value
            isThemeResources(value) -> null
            // Strings and other non-message values fail to parse and stay as they are.
            else -> runCatching { removeThemeResources(value) }.getOrDefault(value)
        }
    }

// The resources name their theme in field 5; elements only reference it in other fields.
private fun isThemeResources(value: ByteArray): Boolean =
    runCatching {
            Proto.fields(value).bytes(5)?.let { Proto.fields(it).string(1) }?.startsWith(THEME_PREFIX)
        }
        .getOrNull() == true
