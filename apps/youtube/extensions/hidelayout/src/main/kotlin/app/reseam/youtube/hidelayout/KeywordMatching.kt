// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout

private val commonBufferText =
    listOf(
        "googlevideo.com/initplayback?source=youtube",
        "ANDROID",
        "https://i.ytimg.com/vi/",
        "mqdefault.jpg",
        "hqdefault.jpg",
        "sddefault.jpg",
        "hq720.jpg",
        "webp",
        "_custom_",
        "OMX.google.vp9.decoder",
        "OMX.google.av1.decoder",
        "OMX.sprd.av1.decoder",
        "c2.android.av1.decoder",
        "c2.android.vp9.decoder",
        "searchR",
        "browse-feed",
        "FEwhat_to_watch",
        "FEsubscriptions",
        "search_vwc_description_transition_key",
        "g-high-recZ",
        "expandable_metadata.e",
        "thumbnail.e",
        "avatar.e",
        "overflow_button.e",
        "shorts-lockup-image",
        "shorts-lockup.overlay-metadata.secondary-text",
        "YouTubeSans-SemiBold",
        "sans-serif",
    )

internal fun titleCase(value: String): String {
    if (value.isEmpty()) return value
    val first = value.codePointAt(0)
    return buildString {
        appendCodePoint(Character.toTitleCase(first))
        append(value, Character.charCount(first), value.length)
    }
}

internal fun capitalizeWords(value: String): String =
    buildString(value.length) {
        var next = true
        var index = 0
        while (index < value.length) {
            var codePoint = value.codePointAt(index)
            if (codePoint == ' '.code) next = true
            else if (next) {
                codePoint = Character.toUpperCase(codePoint)
                next = false
            }
            appendCodePoint(codePoint)
            index += Character.charCount(codePoint)
        }
    }

private val unspacedLanguages =
    setOf(
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.THAI,
        Character.UnicodeBlock.LAO,
        Character.UnicodeBlock.MYANMAR,
        Character.UnicodeBlock.KHMER,
        Character.UnicodeBlock.TIBETAN,
    )

internal fun isLanguageWithNoSpaces(value: String): Boolean =
    value.codePoints().anyMatch { Character.UnicodeBlock.of(it) in unspacedLanguages }

internal fun phrasesWillHideAllVideos(phrases: List<String>, match: KeywordMatch): Boolean =
    phrases.any { phrase ->
        val phraseBytes = phrase.toByteArray(Charsets.UTF_8)
        commonBufferText.any { common ->
            generateSequence(common.indexOf(phrase)) { common.indexOf(phrase, it + 1) }
                .takeWhile { it >= 0 }
                .any { match.accepts(common.toByteArray(Charsets.UTF_8), it, phraseBytes.size) }
        }
    }

internal fun isWholeWord(data: ByteArray, start: Int, length: Int): Boolean {
    val before = utf8Before(data, start)
    val after = utf8At(data, start + length)
    return (before < 0 || !Character.isLetter(before)) && (after < 0 || !Character.isLetter(after))
}

private fun utf8Before(data: ByteArray, index: Int): Int =
    (1..minOf(4, index)).firstNotNullOfOrNull { count ->
        val start = index - count
        utf8At(data, start).takeIf { it >= 0 && utf8ByteCount(data[start]) == count }
    } ?: -1

private fun utf8At(data: ByteArray, index: Int): Int {
    if (index < 0 || index >= data.size) return -1
    val first = data[index].toInt() and 0xFF
    if (first and 0x80 == 0) return first
    val count = utf8ByteCount(first.toByte())
    if (count == 0 || index + count > data.size) return -1
    var value = first and ((1 shl (7 - count)) - 1)
    for (offset in 1 until count) {
        val next = data[index + offset].toInt() and 0xFF
        if (next and 0xC0 != 0x80) return -1
        value = (value shl 6) or (next and 0x3F)
    }
    return value
}

private fun utf8ByteCount(first: Byte): Int {
    val value = first.toInt() and 0xFF
    return when {
        value and 0x80 == 0 -> 1
        value and 0xE0 == 0xC0 -> 2
        value and 0xF0 == 0xE0 -> 3
        value and 0xF8 == 0xF0 -> 4
        else -> 0
    }
}
