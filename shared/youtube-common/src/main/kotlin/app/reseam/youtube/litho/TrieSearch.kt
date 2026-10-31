// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho

/** Receives a candidate span and caller state; false resumes the search. */
internal typealias MatchCallback<T, P> = (text: T, start: Int, length: Int, parameter: P) -> Boolean

/**
 * Searches substrings through a compressed trie, in text order and then callback registration
 * order.
 */
abstract class TrieSearch<T, P> internal constructor(private val root: TrieNode<T, P>) {
    private val registeredPatterns = mutableListOf<T>()

    fun addPattern(pattern: T) {
        addPattern(pattern, root.textLength(pattern), null)
    }

    /** A callback returning false continues searching other matches. */
    fun addPattern(pattern: T, callback: MatchCallback<T, P>) {
        addPattern(pattern, root.textLength(pattern), callback)
    }

    private fun addPattern(pattern: T, length: Int, callback: MatchCallback<T, P>?) {
        if (length == 0) return
        registeredPatterns.add(pattern)
        root.addPattern(pattern, 0, length, callback)
    }

    fun matches(textToSearch: T?, callbackParameter: P): Boolean {
        if (textToSearch == null || registeredPatterns.isEmpty()) return false
        val endIndex = root.textLength(textToSearch)
        for (start in 0 until endIndex) if (
            root.matches(textToSearch, start, endIndex, callbackParameter)
        )
            return true
        return false
    }

    val patterns: List<T>
        get() = registeredPatterns
}

fun <T> TrieSearch<T, Unit>.matches(text: T?): Boolean = matches(text, Unit)
