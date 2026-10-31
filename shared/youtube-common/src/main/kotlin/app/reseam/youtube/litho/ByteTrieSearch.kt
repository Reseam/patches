// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho

class ByteTrieSearch<P> : TrieSearch<ByteArray, P>(ByteNode())

fun convertStringsToBytes(vararg strings: String): Array<ByteArray> =
    Array(strings.size) { strings[it].toByteArray(Charsets.UTF_8) }

private class ByteNode<P>(value: Char = '\u0000') : TrieNode<ByteArray, P>(value) {
    override fun createNode(value: Char): TrieNode<ByteArray, P> = ByteNode(value)

    override fun charValue(text: ByteArray, index: Int): Char = text[index].toInt().toChar()

    override fun textLength(text: ByteArray): Int = text.size
}
