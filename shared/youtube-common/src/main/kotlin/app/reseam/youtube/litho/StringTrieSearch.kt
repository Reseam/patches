// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho

class StringTrieSearch<P> : TrieSearch<String, P>(StringNode())

private class StringNode<P>(value: Char = '\u0000') : TrieNode<String, P>(value) {
    override fun createNode(value: Char): TrieNode<String, P> = StringNode(value)

    override fun charValue(text: String, index: Int): Char = text[index]

    override fun textLength(text: String): Int = text.length
}
