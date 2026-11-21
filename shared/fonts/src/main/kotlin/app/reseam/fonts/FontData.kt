// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.fonts

import java.nio.ByteBuffer

/** What the font's own tables say about it: which characters it maps, its pitch and its axes. */
internal class FontData(buffer: ByteBuffer, ttcIndex: Int) {
    private val data = buffer.duplicate()
    private val tables: Map<String, Int>

    init {
        val start = if (tag(0) == "ttcf") u32(12 + 4 * ttcIndex) else 0
        tables = (0 until u16(start + 4)).associate { tag(start + 12 + 16 * it) to u32(start + 12 + 16 * it + 8) }
    }

    private val cmap = cmapSubtable()

    /** Mapped characters; text fonts map hundreds, math and number fonts a hundred or so. */
    val characters: Int = cmap?.let(::characterCount) ?: 0

    val fixedPitch: Boolean = tables["post"]?.let { u32(it + 12) != 0 } ?: false

    val weightAxis: Boolean = tables["fvar"]?.let { fvar ->
        val axes = fvar + u16(fvar + 4)
        (0 until u16(fvar + 8)).any { tag(axes + it * u16(fvar + 10)) == "wght" }
    } ?: false

    /** A font for reading text: every Latin letter, a full character set, letters of differing widths. */
    val isText: Boolean
        get() = !fixedPitch && characters >= 200 && LATIN.all { glyph(it.code) != 0 }

    private fun cmapSubtable(): Int? {
        val cmap = tables["cmap"] ?: return null
        val subtables = (0 until u16(cmap + 2)).map { cmap + 4 + 8 * it }
            .associate { (u16(it) to u16(it + 2)) to cmap + u32(it + 4) }
        return PREFERRED_ENCODINGS.firstNotNullOfOrNull { subtables[it]?.takeIf { table -> u16(table) in SUPPORTED_FORMATS } }
    }

    private fun characterCount(table: Int): Int = when (u16(table)) {
        12 -> (0 until u32(table + 12)).sumOf { u32(table + 20 + 12 * it) - u32(table + 16 + 12 * it) + 1 }
        else -> (0 until u16(table + 6) / 2).sumOf { segment ->
            val (start, end) = segment4(table, segment)
            if (start == 0xFFFF) 0 else end - start + 1
        }
    }

    private fun glyph(code: Int): Int {
        val table = cmap ?: return 0
        return when (u16(table)) {
            12 -> (0 until u32(table + 12)).firstNotNullOfOrNull { group ->
                val at = table + 16 + 12 * group
                (u32(at + 8) + code - u32(at)).takeIf { code in u32(at)..u32(at + 4) }
            } ?: 0
            else -> {
                val segments = u16(table + 6) / 2
                val segment = (0 until segments).firstOrNull { code in segment4(table, it).let { (s, e) -> s..e } } ?: return 0
                val delta = u16(table + 16 + 4 * segments + 2 * segment)
                val rangeAt = table + 16 + 6 * segments + 2 * segment
                val rangeOffset = u16(rangeAt)
                if (rangeOffset == 0) return (code + delta) and 0xFFFF
                val indexed = u16(rangeAt + rangeOffset + 2 * (code - segment4(table, segment).first))
                if (indexed == 0) 0 else (indexed + delta) and 0xFFFF
            }
        }
    }

    private fun segment4(table: Int, segment: Int): Pair<Int, Int> {
        val segments = u16(table + 6) / 2
        return u16(table + 16 + 2 * segments + 2 * segment) to u16(table + 14 + 2 * segment)
    }

    private fun u16(at: Int) = data.getShort(at).toInt() and 0xFFFF

    private fun u32(at: Int) = data.getInt(at)

    private fun tag(at: Int) = String(ByteArray(4) { data.get(at + it) }, Charsets.US_ASCII)

    private companion object {
        val LATIN = ('a'..'z') + ('A'..'Z')
        val PREFERRED_ENCODINGS = listOf(3 to 10, 0 to 6, 0 to 4, 3 to 1, 0 to 3)
        val SUPPORTED_FORMATS = setOf(4, 12)
    }
}
