// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.savetodevice

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

// Matroska element IDs, marker bits included.
private const val EBML = 0x1A45DFA3L
private const val SEGMENT = 0x18538067L
private const val SEEK_HEAD = 0x114D9B74L
private const val SEEK = 0x4DBBL
private const val SEEK_ID = 0x53ABL
private const val SEEK_POSITION = 0x53ACL
private const val INFO = 0x1549A966L
private const val TIMECODE_SCALE = 0x2AD7B1L
private const val DURATION = 0x4489L
private const val TRACKS = 0x1654AE6BL
private const val TRACK_ENTRY = 0xAEL
private const val TRACK_NUMBER = 0xD7L
private const val TRACK_TYPE = 0x83L
private const val TRACK_TYPE_VIDEO = 1L
private const val CUES = 0x1C53BB6BL
private const val CUE_POINT = 0xBBL
private const val CUE_TIME = 0xB3L
private const val CUE_TRACK_POSITIONS = 0xB7L
private const val CUE_TRACK = 0xF7L
private const val CUE_CLUSTER_POSITION = 0xF1L
private const val CLUSTER = 0x1F43B675L
private const val TIMECODE = 0xE7L
private const val SIMPLE_BLOCK = 0xA3L
private const val BLOCK_GROUP = 0xA0L
private const val BLOCK = 0xA1L

private const val MAX_HEADER = 12
private const val POSITION_WIDTH = 8

private class Element(val id: Long, val start: Long, val dataStart: Long, val end: Long) {
    val size
        get() = end - dataStart
}

private class Cluster(val source: WebmSource, val start: Long, val end: Long, val timecode: Long)

private class WebmSource(val stream: CachedStream) {
    val header: ByteArray
    val info: ByteArray
    val entry: ByteArray
    val trackNumber: Long
    val video: Boolean
    val timecodeScale: Long
    val duration: Double
    val clusters = mutableListOf<Cluster>()

    init {
        val ebml = stream.element(0)
        if (ebml.id != EBML) throw IOException("Not a WebM stream")
        header = stream.read(0, ebml.end.toInt())
        val segment = stream.element(ebml.end)
        if (segment.id != SEGMENT) throw IOException("No segment")
        var info: ByteArray? = null
        var tracks: ByteArray? = null
        var position = segment.dataStart
        while (position < segment.end) {
            val element = stream.element(position)
            when (element.id) {
                INFO -> info = stream.read(element.start, (element.end - element.start).toInt())
                TRACKS -> tracks = stream.read(element.start, (element.end - element.start).toInt())
                CLUSTER -> {
                    val first = stream.element(element.dataStart)
                    if (first.id != TIMECODE) throw IOException("A cluster does not start with its timecode")
                    clusters += Cluster(this, element.start, element.end, uint(stream.read(first.dataStart, first.size.toInt())))
                }
            }
            position = element.end
        }
        this.info = info ?: throw IOException("No segment info")
        val entries = contents(tracks ?: throw IOException("No tracks")).filter { it.id == TRACK_ENTRY }
        val entry = entries.singleOrNull() ?: throw IOException("A stream has ${entries.size} tracks")
        val trackBytes = checkNotNull(tracks)
        this.entry = trackBytes.copyOfRange(entry.start.toInt(), entry.end.toInt())
        val fields = contents(this.entry)
        trackNumber = uint(fields.value(this.entry, TRACK_NUMBER) ?: throw IOException("No track number"))
        video = fields.value(this.entry, TRACK_TYPE)?.let(::uint) == TRACK_TYPE_VIDEO
        val infoFields = contents(this.info)
        timecodeScale = infoFields.value(this.info, TIMECODE_SCALE)?.let(::uint) ?: 1_000_000L
        duration = infoFields.value(this.info, DURATION)?.let(::float) ?: 0.0
    }
}

internal fun writeWebm(streams: List<CachedStream>, output: OutputStream, onProgress: (Float) -> Unit) {
    val sources = streams.map(::WebmSource)
    if (sources.map { it.timecodeScale }.distinct().size != 1) throw IOException("The streams use different timecode scales")
    val clusters = sources.flatMap { it.clusters }.sortedBy { it.timecode }
    val numbers = sources.withIndex().associate { (index, source) -> source to index + 1L }
    val cueSource = sources.firstOrNull { it.video } ?: sources.first()
    val info = sources.maxBy { it.duration }.info
    val tracks = element(TRACKS, sources.fold(ByteArray(0)) { all, source -> all + renumbered(source.entry, numbers.getValue(source)) })

    fun cues(positions: Map<Cluster, Long>): ByteArray =
        element(
            CUES,
            cueSource.clusters.fold(ByteArray(0)) { all, cluster ->
                val position = element(CUE_TRACK_POSITIONS, uint(CUE_TRACK, numbers.getValue(cueSource), 1) + uint(CUE_CLUSTER_POSITION, positions[cluster] ?: 0))
                all + element(CUE_POINT, uint(CUE_TIME, cluster.timecode) + position)
            },
        )

    fun seekHead(infoAt: Long, tracksAt: Long, cuesAt: Long): ByteArray =
        element(SEEK_HEAD, seek(INFO, infoAt) + seek(TRACKS, tracksAt) + seek(CUES, cuesAt))

    // Positions are fixed-width, so every element's size is known before the positions are.
    val seekSize = seekHead(0, 0, 0).size.toLong()
    val cuesAt = seekSize + info.size + tracks.size
    val clustersAt = cuesAt + cues(emptyMap()).size
    val positions = HashMap<Cluster, Long>()
    clusters.fold(clustersAt) { position, cluster -> position.also { positions[cluster] = it } + (cluster.end - cluster.start) }
    val clusterBytes = clusters.sumOf { it.end - it.start }

    output.write(sources.first().header)
    output.write(id(SEGMENT))
    output.write(ByteBuffer.allocate(8).putLong(clustersAt + clusterBytes).array().also { it[0] = 1 })
    output.write(seekHead(seekSize, seekSize + info.size, cuesAt))
    output.write(info)
    output.write(tracks)
    output.write(cues(positions))
    val buffer = ByteArray(COPY_BUFFER)
    var copied = 0L
    for (cluster in clusters) {
        val number = numbers.getValue(cluster.source)
        val size = cluster.end - cluster.start
        if (number == cluster.source.trackNumber) cluster.source.stream.copyTo(output, cluster.start, size, buffer)
        else output.write(renumberBlocks(cluster.source.stream.read(cluster.start, size.toInt()), cluster.source.trackNumber, number))
        copied += size
        onProgress(copied.toFloat() / clusterBytes)
    }
    output.flush()
}

private fun renumbered(entry: ByteArray, number: Long): ByteArray {
    val fields = contents(entry)
    val body = ByteArrayOutputStream()
    for (field in fields) {
        if (field.id == TRACK_NUMBER) body.write(uint(TRACK_NUMBER, number, 1))
        else body.write(entry, field.start.toInt(), (field.end - field.start).toInt())
    }
    return element(TRACK_ENTRY, body.toByteArray())
}

// Each block starts with its track number.
private fun renumberBlocks(cluster: ByteArray, from: Long, to: Long): ByteArray {
    if (from > 126 || to > 126) throw IOException("Track numbers beyond one byte")
    fun patch(start: Int, end: Int) {
        for (child in children(cluster, start, end)) {
            when (child.id) {
                SIMPLE_BLOCK, BLOCK -> {
                    val at = child.dataStart.toInt()
                    if (cluster[at].toLong() and 0xFF != (0x80 or from.toInt()).toLong()) throw IOException("A block of another track")
                    cluster[at] = (0x80 or to.toInt()).toByte()
                }
                BLOCK_GROUP -> patch(child.dataStart.toInt(), child.end.toInt())
            }
        }
    }
    val top = parseElement(cluster, 0, 0)
    patch(top.dataStart.toInt(), top.end.toInt())
    return cluster
}

private fun CachedStream.element(position: Long): Element {
    val header = read(position, minOf(MAX_HEADER.toLong(), length - position).toInt())
    return parseElement(header, 0, position)
}

private fun parseElement(bytes: ByteArray, offset: Int, position: Long): Element {
    val (id, idLength) = vint(bytes, offset, keepMarker = true)
    val (size, sizeLength) = vint(bytes, offset + idLength, keepMarker = false)
    if (size == (1L shl (7 * sizeLength)) - 1) throw IOException("An element of unknown size")
    val dataStart = position + idLength + sizeLength
    return Element(id, position, dataStart, dataStart + size)
}

private fun children(bytes: ByteArray, start: Int, end: Int): List<Element> {
    val elements = mutableListOf<Element>()
    var position = start
    while (position < end) {
        val element = parseElement(bytes, position, position.toLong())
        if (element.end > end) throw IOException("Malformed element")
        elements += element
        position = element.end.toInt()
    }
    return elements
}

private fun contents(bytes: ByteArray): List<Element> {
    val top = parseElement(bytes, 0, 0)
    return children(bytes, top.dataStart.toInt(), top.end.toInt())
}

private fun List<Element>.value(bytes: ByteArray, id: Long): ByteArray? =
    firstOrNull { it.id == id }?.let { bytes.copyOfRange(it.dataStart.toInt(), it.end.toInt()) }

private fun vint(bytes: ByteArray, offset: Int, keepMarker: Boolean): Pair<Long, Int> {
    val first = bytes[offset].toInt() and 0xFF
    val length = Integer.numberOfLeadingZeros(first) - 23
    if (length !in 1..8) throw IOException("Malformed element header")
    var value = (if (keepMarker) first else first and (0xFF shr length)).toLong()
    for (index in 1 until length) value = (value shl 8) or (bytes[offset + index].toLong() and 0xFF)
    return value to length
}

private fun uint(bytes: ByteArray): Long = bytes.fold(0L) { value, byte -> (value shl 8) or (byte.toLong() and 0xFF) }

private fun float(bytes: ByteArray): Double =
    if (bytes.size == 4) ByteBuffer.wrap(bytes).float.toDouble() else ByteBuffer.wrap(bytes).double

private fun id(id: Long): ByteArray {
    val length = (64 - java.lang.Long.numberOfLeadingZeros(id) + 7) / 8
    return ByteArray(length) { (id shr (8 * (length - 1 - it))).toByte() }
}

private fun element(id: Long, payload: ByteArray): ByteArray = id(id) + size(payload.size.toLong()) + payload

private fun uint(id: Long, value: Long, width: Int = POSITION_WIDTH): ByteArray =
    element(id, ByteArray(width) { (value shr (8 * (width - 1 - it))).toByte() })

private fun seek(id: Long, position: Long): ByteArray = element(SEEK, element(SEEK_ID, id(id)) + uint(SEEK_POSITION, position))

// The shortest size field for [size]; the all-ones value of each length means unknown, so it is skipped.
private fun size(size: Long): ByteArray {
    val length = (1..8).first { size < (1L shl (7 * it)) - 1 }
    val marked = size or (1L shl (7 * length))
    return ByteArray(length) { (marked shr (8 * (length - 1 - it))).toByte() }
}
