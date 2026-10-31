// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout

import app.reseam.bundle.Logger
import app.reseam.youtube.core.debug
import java.util.Locale

internal enum class KeywordMatch {
    SUBSTRING,
    WHOLE_WORD;

    fun accepts(bytes: ByteArray, start: Int, length: Int): Boolean =
        this == SUBSTRING || isWholeWord(bytes, start, length)
}

internal fun parseKeywords(raw: String): Map<String, KeywordMatch> {
    val keywords = linkedMapOf<String, KeywordMatch>()
    val conflicting = hashSetOf<String>()
    for (line in raw.split('\n')) {
        val trimmed = line.trimEnd { Character.isWhitespace(it) }
        if (trimmed.isEmpty()) continue
        val wholeWord = trimmed.length >= 2 && trimmed.startsWith('"') && trimmed.endsWith('"')
        val match = if (wholeWord) KeywordMatch.WHOLE_WORD else KeywordMatch.SUBSTRING
        val phrase = if (wholeWord) trimmed.substring(1, trimmed.length - 1) else trimmed
        val validLength =
            when {
                phrase.isEmpty() -> false
                wholeWord || phrase.length >= 3 -> true
                else -> isLanguageWithNoSpaces(phrase)
            }
        if (!validLength) {
            Logger.debug { "KeywordContentFilter ignored a short phrase" }
            continue
        }
        val variants =
            listOf(
                phrase,
                phrase.lowercase(Locale.ROOT),
                titleCase(phrase),
                capitalizeWords(phrase),
                phrase.uppercase(Locale.ROOT),
            )
        if (phrasesWillHideAllVideos(variants, match)) {
            Logger.debug { "KeywordContentFilter ignored a phrase that matches every video" }
            continue
        }
        for (variant in variants) {
            if (variant in conflicting) continue
            val previous = keywords[variant]
            when {
                previous == null -> keywords[variant] = match
                previous != match -> {
                    keywords.remove(variant)
                    conflicting.add(variant)
                    Logger.debug { "KeywordContentFilter ignored conflicting phrase: $variant" }
                }
            }
        }
    }
    return keywords
}
