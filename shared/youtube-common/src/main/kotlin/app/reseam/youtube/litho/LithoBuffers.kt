// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho

private val emptyBuffer = ByteArray(0)
private val currentTree = ThreadLocal<Tree>()
private val threadBuffers = ThreadLocal<MutableMap<String, ByteArray>>()
private val globalBuffers = LinkedHashMap<String, ByteArray>(200)

/** Retains identified buffers in the global and calling-thread caches; null is ignored. */
internal fun rememberProtoBuffer(buffer: ByteArray?) {
    if (buffer == null) return
    val identifier = identifierOf(buffer) ?: return
    synchronized(globalBuffers) { globalBuffers.remember(identifier, buffer) }
    val local =
        threadBuffers.get() ?: LinkedHashMap<String, ByteArray>(200).also(threadBuffers::set)
    local.remember(identifier, buffer)
}

internal fun enterBufferTree(root: Any?, identifier: String?): Any? =
    currentTree.get().also { outer ->
        currentTree.set(Tree(root, identifier?.takeIf { '|' in it }?.substringBefore('|'), outer))
    }

internal fun exitBufferTree(outer: Any?) {
    currentTree.set(outer as Tree?)
}

/**
 * Prefers tree buffers, then the thread cache, then the global cache; misses return an empty
 * buffer.
 */
internal fun bufferFor(identifier: String): ByteArray {
    val pipe = identifier.indexOf('|')
    if (pipe < 0) return emptyBuffer
    val key = identifier.substring(0, pipe)
    // Templates nest: prefer the innermost tree rooted at this identifier.
    return generateSequence(currentTree.get()) { it.outer }
        .firstNotNullOfOrNull { it.bufferFor(key) }
        ?: threadBuffers.get()?.get(key)
        ?: synchronized(globalBuffers) { globalBuffers[key] }
        ?: emptyBuffer
}

private class Tree(root: Any?, private val identifierKey: String?, val outer: Tree?) {
    private var encoding: Encoding = Encoding.Pending(root)

    fun bufferFor(key: String): ByteArray? {
        if (key != identifierKey) return null
        val pending = encoding as? Encoding.Pending
        if (pending != null) {
            // Mark attempted before calling app code: failures and reentry must not retry it.
            encoding = Encoding.Encoded(null)
            val bytes = encodeElement(pending.root)
            encoding = Encoding.Encoded(bytes)
            val encodedIdentifier = bytes?.let(::identifierOf)
            if (encodedIdentifier != null && encodedIdentifier != key)
                encoding = Encoding.Encoded(null)
        }
        return (encoding as Encoding.Encoded).bytes
    }
}

private sealed interface Encoding {
    data class Pending(val root: Any?) : Encoding

    data class Encoded(val bytes: ByteArray?) : Encoding
}

private fun MutableMap<String, ByteArray>.remember(key: String, buffer: ByteArray) {
    // Preserve insertion-order eviction, including when an existing identifier is updated.
    put(key, buffer)
    if (size > 100) remove(keys.first())
}
