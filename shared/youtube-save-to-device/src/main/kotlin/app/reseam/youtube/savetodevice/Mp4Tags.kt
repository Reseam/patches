// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.savetodevice

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel

internal class Tags(
    val title: String,
    val artist: String?,
    val album: String?,
    val track: Pair<Int, Int>?,
    val cover: Cover?,
)

internal class Cover(val bytes: ByteArray, val png: Boolean)

private val CONTAINERS = setOf("moov", "trak", "mdia", "minf", "stbl", "udta", "edts")
private val DROPPED = setOf("ftyp", "moov", "free", "skip")
private const val UINT32_MAX = 0xFFFFFFFFL

// iTunes metadata well-known data types.
private const val DATA_IMPLICIT = 0
private const val DATA_UTF8 = 1
private const val DATA_JPEG = 13
private const val DATA_PNG = 14

private class Box(val type: String, val start: Long, val headerSize: Int, val size: Long) {
    val end
        get() = start + size
}

private class Node(val type: String, val payload: ByteArray, val children: MutableList<Node>? = null)

/**
 * Writes [source], an MP4 as MediaMuxer produced it, to [output] with the movie box in front of the media and
 * [tags] in it. Chunk offsets move with the data they point to, so any source layout works.
 */
internal fun writeTagged(source: FileChannel, output: OutputStream, tags: Tags) {
    val boxes = topLevelBoxes(source)
    val ftyp = boxes.firstOrNull { it.type == "ftyp" } ?: throw IOException("No ftyp box")
    val moovBox = boxes.firstOrNull { it.type == "moov" } ?: throw IOException("No moov box")
    val media = boxes.filter { it.type !in DROPPED }
    val moov = parse("moov", read(source, moovBox.start + moovBox.headerSize, moovBox.size - moovBox.headerSize))
    addTags(moov, tags)

    fun relocation(wide: Boolean): (Long) -> Long {
        val mediaStart = ftyp.size + serialize(withOffsets(moov, wide) { it }).size
        val starts = media.runningFold(mediaStart) { position, box -> position + box.size }
        return { offset ->
            val index = media.indexOfFirst { offset >= it.start && offset < it.end }
            if (index < 0) throw IOException("A chunk offset points outside the media")
            offset - media[index].start + starts[index]
        }
    }
    // Moving the media can push offsets past 32 bits; wider tables then move it a little further.
    val narrow = relocation(false)
    val wide = chunkOffsets(moov).any { narrow(it) > UINT32_MAX }
    val encoded = serialize(withOffsets(moov, wide, relocation(wide)))

    val sink = Channels.newChannel(output)
    source.copy(ftyp, sink)
    output.write(encoded)
    media.forEach { source.copy(it, sink) }
    output.flush()
}

private fun FileChannel.copy(box: Box, sink: WritableByteChannel) {
    var done = 0L
    while (done < box.size) {
        val count = transferTo(box.start + done, box.size - done, sink)
        if (count <= 0) throw IOException("Short copy")
        done += count
    }
}

private fun topLevelBoxes(channel: FileChannel): List<Box> {
    val boxes = mutableListOf<Box>()
    val length = channel.size()
    var position = 0L
    while (position < length) {
        val header = ByteBuffer.wrap(read(channel, position, minOf(16, length - position)))
        val size32 = header.int.toLong() and UINT32_MAX
        val type = String(header.array(), 4, 4, Charsets.ISO_8859_1)
        header.position(8)
        val (size, headerSize) =
            when (size32) {
                0L -> length - position to 8
                1L -> header.long to 16
                else -> size32 to 8
            }
        if (size < headerSize || position + size > length) throw IOException("Malformed $type box")
        boxes += Box(type, position, headerSize, size)
        position += size
    }
    return boxes
}

private fun read(channel: FileChannel, position: Long, size: Long): ByteArray {
    val buffer = ByteBuffer.allocate(Math.toIntExact(size))
    while (buffer.hasRemaining()) {
        if (channel.read(buffer, position + buffer.position()) < 0) throw IOException("Short read")
    }
    return buffer.array()
}

private fun parse(type: String, payload: ByteArray): Node {
    if (type !in CONTAINERS) return Node(type, payload)
    val children = mutableListOf<Node>()
    val buffer = ByteBuffer.wrap(payload)
    while (buffer.hasRemaining()) {
        val start = buffer.position()
        val size32 = buffer.int.toLong() and UINT32_MAX
        val childType = String(payload, start + 4, 4, Charsets.ISO_8859_1)
        buffer.position(start + 8)
        val (size, headerSize) = if (size32 == 1L) buffer.long to 16 else size32 to 8
        if (size < headerSize || start + size > payload.size) throw IOException("Malformed $childType box")
        val end = (start + size).toInt()
        children += parse(childType, payload.copyOfRange(start + headerSize, end))
        buffer.position(end)
    }
    return Node(type, ByteArray(0), children)
}

private fun serialize(node: Node): ByteArray {
    val body =
        node.children?.fold(ByteArrayOutputStream()) { out, child -> out.apply { write(serialize(child)) } }
            ?.toByteArray() ?: node.payload
    return ByteBuffer.allocate(body.size + 8)
        .putInt(body.size + 8)
        .put(node.type.toByteArray(Charsets.ISO_8859_1))
        .put(body)
        .array()
}

private fun Node.find(type: String): List<Node> =
    children.orEmpty().flatMap { (if (it.type == type) listOf(it) else emptyList()) + it.find(type) }

private fun Node.offsets(): List<Long> {
    val buffer = ByteBuffer.wrap(payload, 4, payload.size - 4)
    return List(buffer.int) { if (type == "stco") buffer.int.toLong() and UINT32_MAX else buffer.long }
}

private fun chunkOffsets(moov: Node): List<Long> = (moov.find("stco") + moov.find("co64")).flatMap { it.offsets() }

// A copy of the tree whose chunk offset tables hold relocated offsets, as co64 when [wide].
private fun withOffsets(node: Node, wide: Boolean, relocate: (Long) -> Long): Node {
    if (node.type == "stco" || node.type == "co64") {
        val offsets = node.offsets().map(relocate)
        val table =
            ByteBuffer.allocate(8 + offsets.size * if (wide) 8 else 4)
                .put(node.payload, 0, 4)
                .putInt(offsets.size)
        offsets.forEach { if (wide) table.putLong(it) else table.putInt(it.toInt()) }
        return Node(if (wide) "co64" else "stco", table.array())
    }
    return Node(node.type, node.payload, node.children?.mapTo(mutableListOf()) { withOffsets(it, wide, relocate) })
}

private fun addTags(moov: Node, tags: Tags) {
    val items = buildList {
        add(item("©nam", DATA_UTF8, tags.title.toByteArray()))
        tags.artist?.let { add(item("©ART", DATA_UTF8, it.toByteArray())) }
        tags.album?.let { add(item("©alb", DATA_UTF8, it.toByteArray())) }
        tags.track?.let { (number, total) ->
            val value = ByteBuffer.allocate(8).putShort(2, number.toShort()).putShort(4, total.toShort())
            add(item("trkn", DATA_IMPLICIT, value.array()))
        }
        tags.cover?.let { add(item("covr", if (it.png) DATA_PNG else DATA_JPEG, it.bytes)) }
    }
    // version and flags, pre_defined, handler type, then three reserved words (Apple writes "appl"), empty name.
    val handler = ByteBuffer.allocate(25).putInt(0).putInt(0).put("mdir".toByteArray()).put("appl".toByteArray())
    val meta =
        Node(
            "meta",
            ByteArray(4) + serialize(Node("hdlr", handler.array())) + serialize(Node("ilst", ByteArray(0), items.toMutableList())),
        )
    val moovChildren = checkNotNull(moov.children)
    val udta = moovChildren.firstOrNull { it.type == "udta" } ?: Node("udta", ByteArray(0), mutableListOf()).also(moovChildren::add)
    val udtaChildren = checkNotNull(udta.children)
    udtaChildren.removeAll { it.type == "meta" }
    udtaChildren += meta
}

private fun item(type: String, dataType: Int, value: ByteArray): Node {
    val data = ByteBuffer.allocate(8 + value.size).putInt(dataType).putInt(0).put(value)
    return Node(type, ByteArray(0), mutableListOf(Node("data", data.array())))
}
