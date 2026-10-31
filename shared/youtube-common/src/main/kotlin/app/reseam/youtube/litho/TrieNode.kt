// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho

internal abstract class TrieNode<T, P>(private val nodeValue: Char = '\u0000') {
    private var leaf: CompressedPath<T, P>? = null
    // A present character hashes perfectly; absent characters may collide and must be compared.
    private var children: Array<TrieNode<T, P>?>? = null
    private var endOfPatternCallbacks: MutableList<MatchCallback<T, P>?>? = null

    fun addPattern(pattern: T, index: Int, length: Int, callback: MatchCallback<T, P>?) {
        if (index == length) {
            val callbacks =
                endOfPatternCallbacks
                    ?: ArrayList<MatchCallback<T, P>?>(1).also {
                        endOfPatternCallbacks = it
                    }
            callbacks.add(callback)
            return
        }
        val existing = leaf
        if (existing != null) {
            // Push the existing compressed path down a level before adding the new pattern.
            check(children == null) { "A compressed suffix has no child table" }
            children = arrayOfNulls(1)
            leaf = null
            addPattern(existing.pattern, existing.start, existing.length, existing.callback)
        } else if (children == null) {
            leaf = CompressedPath(pattern, index, length, callback)
            return
        }
        val table = checkNotNull(children) { "Adding a branch establishes its child table" }
        val character = charValue(pattern, index)
        val slot = character.code % table.size
        val child =
            when (val current = table[slot]) {
                null -> createNode(character).also { table[slot] = it }
                else ->
                    if (current.nodeValue == character) current
                    else createNode(character).also(::expandChildArray)
            }
        child.addPattern(pattern, index + 1, length, callback)
    }

    private fun expandChildArray(child: TrieNode<T, P>) {
        val current = checkNotNull(children) { "Only an existing child table can have collisions" }
        var size = current.size
        while (true) {
            size += 2
            val replacement = arrayOfNulls<TrieNode<T, P>>(size)
            addNodeToArray(replacement, child)
            if (current.any { it != null && !addNodeToArray(replacement, it) }) continue
            children = replacement
            return
        }
    }

    private fun addNodeToArray(array: Array<TrieNode<T, P>?>, child: TrieNode<T, P>): Boolean {
        val slot = child.nodeValue.code % array.size
        if (array[slot] != null) return false
        array[slot] = child
        return true
    }

    // Iterative: the VM does not optimize tail calls, and every litho path walks this tree.
    fun matches(text: T, start: Int, end: Int, parameter: P): Boolean {
        var node = this
        var index = start
        while (true) {
            if (node.leaf?.matches(this, text, end, index, parameter) == true) return true
            if (
                node.endOfPatternCallbacks?.any {
                    it == null || it(text, start, index - start, parameter)
                } == true
            )
                return true
            val table = node.children ?: return false
            if (index == end) return false
            // Use the start node as receiver so the VM sees a single class during this walk.
            val character = charValue(text, index)
            val child = table[character.code % table.size] ?: return false
            if (child.nodeValue != character) return false
            node = child
            index++
        }
    }

    abstract fun createNode(value: Char): TrieNode<T, P>

    abstract fun charValue(text: T, index: Int): Char

    abstract fun textLength(text: T): Int
}

/** A pattern's unbranched suffix shares a single node until another pattern needs to branch. */
private data class CompressedPath<T, P>(
    val pattern: T,
    val start: Int,
    val length: Int,
    val callback: MatchCallback<T, P>?,
) {
    fun matches(node: TrieNode<T, P>, text: T, end: Int, index: Int, parameter: P): Boolean {
        if (end - index < length - start) return false
        for (offset in start until length) if (
            node.charValue(text, index + offset - start) != node.charValue(pattern, offset)
        )
            return false
        return callback?.invoke(text, index - start, length, parameter) ?: true
    }
}
