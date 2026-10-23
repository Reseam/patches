// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.savetodevice

import android.media.MediaDataSource
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

// ExoPlayer's SimpleCache layout: an index of content ids by cache key, and each content's bytes in span
// files named <id>.<position>.<timestamp>.v3.exo, spread over numbered subfolders.
private const val INDEX = "cached_content_index.exi"
private const val INDEX_VERSION = 2
private const val INDEX_FLAG_ENCRYPTED = 1
private const val BLOCK = 16

internal class StreamCacheException(val reason: Reason) : IOException(reason.name) {
    enum class Reason { MISSING, INCOMPLETE }
}

/** Finds the stream's cache folder and content id, and opens its bytes as the app wrote them. */
internal fun openStream(stream: SavedStream, key: SecretKey): CachedStream {
    for (folder in stream.folders) {
        val id = readIndex(folder, key)?.get(stream.cacheKey) ?: continue
        return CachedStream(spans(folder, id, stream.length), stream.cacheKey, key, stream.length)
    }
    throw StreamCacheException(StreamCacheException.Reason.MISSING)
}

private fun readIndex(folder: File, key: SecretKey): Map<String, Int>? {
    // The index is written as an atomic file: while a rewrite is in progress the backup is the valid copy.
    val file = File(folder, "$INDEX.bak").takeIf(File::isFile) ?: File(folder, INDEX).takeIf(File::isFile) ?: return null
    DataInputStream(BufferedInputStream(file.inputStream())).use { header ->
        if (header.readInt() != INDEX_VERSION) throw IOException("Unsupported cache index version")
        val input =
            if (header.readInt() and INDEX_FLAG_ENCRYPTED == 0) header
            else {
                val iv = ByteArray(BLOCK).also(header::readFully)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
                    init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                }
                DataInputStream(CipherInputStream(header, cipher))
            }
        val count = input.readInt()
        return buildMap(count) {
            repeat(count) {
                val id = input.readInt()
                val cacheKey = input.readUTF()
                repeat(input.readInt()) {
                    input.readUTF()
                    input.readFully(ByteArray(input.readInt()))
                }
                put(cacheKey, id)
            }
        }
    }
}

internal class Span(val position: Long, val file: File) {
    val length = file.length()
    val end get() = position + length
}

private fun spans(folder: File, id: Int, length: Long): List<Span> {
    val name = Regex("""$id\.(\d+)\.\d+\.v3\.exo""")
    val spans =
        folder
            .listFiles()
            .orEmpty()
            .filter(File::isDirectory)
            .flatMap { it.listFiles().orEmpty().asList() }
            .mapNotNull { file -> name.matchEntire(file.name)?.let { Span(it.groupValues[1].toLong(), file) } }
            .sortedBy(Span::position)
    var covered = 0L
    for (span in spans) {
        if (span.position != covered) break
        covered = span.end
    }
    if (covered != length) throw StreamCacheException(StreamCacheException.Reason.INCOMPLETE)
    return spans
}

/**
 * Random access to a cached stream. The downloader encrypts content with AES-CTR keyed by the downloads key, the
 * nonce being the FNV-1a hash of the cache key and the counter the block at the byte's position in the content.
 */
internal class CachedStream(
    private val spans: List<Span>,
    cacheKey: String,
    private val key: SecretKey,
    private val length: Long,
) : MediaDataSource() {
    private val nonce = cacheKey.fold(0L) { hash, char -> (hash xor char.code.toLong()) * 0x100000001b3L }
    private var file: RandomAccessFile? = null
    private var span: Span? = null
    private var cipher: Cipher? = null
    private var next = -1L

    override fun getSize(): Long = length

    @Synchronized
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= length) return -1
        if (position != next) seek(position)
        val span = checkNotNull(span)
        val count = minOf(size.toLong(), span.end - position).toInt()
        checkNotNull(file).readFully(buffer, offset, count)
        checkNotNull(cipher).update(buffer, offset, count, buffer, offset)
        next = position + count
        if (next == span.end) next = -1
        return count
    }

    private fun seek(position: Long) {
        val target = spans.last { it.position <= position }
        if (target !== span) {
            file?.close()
            file = RandomAccessFile(target.file, "r")
            span = target
        }
        checkNotNull(file).seek(position - target.position)
        cipher = Cipher.getInstance("AES/CTR/NoPadding").apply {
            val counter = ByteBuffer.allocate(BLOCK).putLong(nonce).putLong(position / BLOCK).array()
            init(Cipher.DECRYPT_MODE, key, IvParameterSpec(counter))
            val within = (position % BLOCK).toInt()
            if (within > 0) update(ByteArray(within))
        }
        next = position
    }

    @Synchronized
    override fun close() {
        file?.close()
        file = null
        span = null
        next = -1
    }
}
