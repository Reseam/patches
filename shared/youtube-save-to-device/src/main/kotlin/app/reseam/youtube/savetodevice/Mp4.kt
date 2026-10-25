// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.savetodevice

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

private val CONTAINERS = setOf("moov", "trak", "mdia", "minf", "stbl", "udta", "edts", "mvex", "moof", "traf")
private const val UINT32_MAX = 0xFFFFFFFFL
private const val BOX_HEADER = 8
private const val LARGE_BOX_HEADER = 16

// tfhd flags.
private const val BASE_DATA_OFFSET = 0x1
private const val SAMPLE_DESCRIPTION_INDEX = 0x2
private const val DEFAULT_DURATION = 0x8
private const val DEFAULT_SIZE = 0x10
private const val DEFAULT_FLAGS = 0x20

// trun flags.
private const val DATA_OFFSET = 0x1
private const val FIRST_SAMPLE_FLAGS = 0x4
private const val SAMPLE_DURATION = 0x100
private const val SAMPLE_SIZE = 0x200
private const val SAMPLE_FLAGS = 0x400
private const val SAMPLE_COMPOSITION_OFFSET = 0x800

private const val NON_SYNC_SAMPLE = 0x10000

internal class Node(val type: String, val payload: ByteArray, val children: MutableList<Node>? = null) {
    fun child(type: String): Node? = children?.firstOrNull { it.type == type }

    fun require(type: String): Node = child(type) ?: throw IOException("No $type box")
}

internal fun container(type: String, children: List<Node>): Node = Node(type, ByteArray(0), children.toMutableList())

internal fun serialize(node: Node): ByteArray {
    val body =
        node.children?.fold(ByteArrayOutputStream()) { out, child -> out.apply { write(serialize(child)) } }
            ?.toByteArray() ?: node.payload
    return ByteBuffer.allocate(body.size + BOX_HEADER)
        .putInt(body.size + BOX_HEADER)
        .put(node.type.toByteArray(Charsets.ISO_8859_1))
        .put(body)
        .array()
}

private fun parse(type: String, payload: ByteArray): Node {
    if (type !in CONTAINERS) return Node(type, payload)
    val children = mutableListOf<Node>()
    val buffer = ByteBuffer.wrap(payload)
    while (buffer.hasRemaining()) {
        val start = buffer.position()
        val size32 = buffer.int.toLong() and UINT32_MAX
        val childType = String(payload, start + 4, 4, Charsets.ISO_8859_1)
        buffer.position(start + BOX_HEADER)
        val (size, headerSize) = if (size32 == 1L) buffer.long to LARGE_BOX_HEADER else size32 to BOX_HEADER
        if (size < headerSize || start + size > payload.size) throw IOException("Malformed $childType box")
        val end = (start + size).toInt()
        children += parse(childType, payload.copyOfRange(start + headerSize, end))
        buffer.position(end)
    }
    return Node(type, ByteArray(0), children)
}

private class Sample(val size: Int, val duration: Long, val sync: Boolean, val compositionOffset: Int)

private class Chunk(val track: Track, val position: Long, val size: Long, val samples: Int, val decodeTime: Long) {
    val seconds
        get() = decodeTime.toDouble() / track.timescale
}

private class Track(val stream: CachedStream, val trak: Node, val movieHeader: ByteArray) {
    val id = ByteBuffer.wrap(trak.require("tkhd").payload).let { it.getInt(if (it.get(0).toInt() == 1) 20 else 12) }
    val timescale = timescale(trak.require("mdia").require("mdhd").payload)
    val samples = mutableListOf<Sample>()
    val chunks = mutableListOf<Chunk>()

    init {
        // A fragmented file's tables are empty; its samples come with its fragments.
        readSampleTables(trak.require("mdia").require("minf").require("stbl"))
    }

    val duration
        get() = samples.sumOf { it.duration }

    private fun readSampleTables(stbl: Node) {
        val sizes = ByteBuffer.wrap(stbl.require("stsz").payload)
        val fixedSize = sizes.getInt(4)
        val count = sizes.getInt(8)
        val durations = expandRuns(stbl.require("stts").payload).map { it and UINT32_MAX }
        val compositionOffsets = stbl.child("ctts")?.let { expandRuns(it.payload) }
        val syncSamples = stbl.child("stss")?.let { entries(it.payload, 4) { buffer -> buffer.int } }?.toSet()
        val chunkOffsets =
            stbl.child("co64")?.let { entries(it.payload, 8) { buffer -> buffer.long } }
                ?: entries(stbl.require("stco").payload, 4) { buffer -> buffer.int.toLong() and UINT32_MAX }
        // stsc: first chunk (from 1), samples per chunk, description index.
        val sampleRuns = entries(stbl.require("stsc").payload, 12) { buffer -> (buffer.int to buffer.int).also { buffer.int } }
        var sample = 0
        var decodeTime = 0L
        chunkOffsets.forEachIndexed { index, position ->
            val perChunk = sampleRuns.last { (firstChunk, _) -> firstChunk <= index + 1 }.second
            val startTime = decodeTime
            var size = 0L
            repeat(perChunk) {
                if (sample >= count) throw IOException("The sample tables disagree")
                val sampleSize = if (fixedSize != 0) fixedSize else sizes.getInt(12 + sample * 4)
                val duration = durations[sample]
                samples += Sample(sampleSize, duration, syncSamples?.contains(sample + 1) ?: true, compositionOffsets?.get(sample)?.toInt() ?: 0)
                size += sampleSize
                decodeTime += duration
                sample++
            }
            chunks += Chunk(this, position, size, perChunk, startTime)
        }
    }

    fun readFragment(traf: Node, moofStart: Long, trex: ByteBuffer?) {
        val tfhd = ByteBuffer.wrap(traf.require("tfhd").payload)
        val tfhdFlags = tfhd.int and 0xFFFFFF
        tfhd.int
        val base = if (tfhdFlags and BASE_DATA_OFFSET != 0) tfhd.long else moofStart
        if (tfhdFlags and SAMPLE_DESCRIPTION_INDEX != 0) tfhd.int
        val defaultDuration = if (tfhdFlags and DEFAULT_DURATION != 0) tfhd.int else trex?.getInt(12) ?: 0
        val defaultSize = if (tfhdFlags and DEFAULT_SIZE != 0) tfhd.int else trex?.getInt(16) ?: 0
        val defaultFlags = if (tfhdFlags and DEFAULT_FLAGS != 0) tfhd.int else trex?.getInt(20) ?: 0
        var decodeTime =
            traf.child("tfdt")?.let { ByteBuffer.wrap(it.payload) }?.let { tfdt ->
                if (tfdt.get(0).toInt() == 1) tfdt.getLong(4) else tfdt.getInt(4).toLong() and UINT32_MAX
            } ?: duration
        var dataPosition = base
        for (trun in traf.children.orEmpty().filter { it.type == "trun" }) {
            val run = ByteBuffer.wrap(trun.payload)
            val flags = run.int and 0xFFFFFF
            val count = run.int
            if (flags and DATA_OFFSET != 0) dataPosition = base + run.int
            val firstFlags = if (flags and FIRST_SAMPLE_FLAGS != 0) run.int else defaultFlags
            val start = dataPosition
            val startTime = decodeTime
            repeat(count) { index ->
                val sampleDuration =
                    (if (flags and SAMPLE_DURATION != 0) run.int else defaultDuration).toLong() and UINT32_MAX
                val size = if (flags and SAMPLE_SIZE != 0) run.int else defaultSize
                val sampleFlags =
                    if (flags and SAMPLE_FLAGS != 0) run.int else if (index == 0) firstFlags else defaultFlags
                val compositionOffset = if (flags and SAMPLE_COMPOSITION_OFFSET != 0) run.int else 0
                samples += Sample(size, sampleDuration, sampleFlags and NON_SYNC_SAMPLE == 0, compositionOffset)
                dataPosition += size
                decodeTime += sampleDuration
            }
            chunks += Chunk(this, start, dataPosition - start, count, startTime)
        }
    }
}

// A stream's tracks: a DASH stream carries one, a progressive file its video and audio together.
private fun tracks(stream: CachedStream): List<Track> {
    var tracks: List<Track>? = null
    var defaults = emptyMap<Int, ByteBuffer>()
    var position = 0L
    while (position < stream.length) {
        val header = ByteBuffer.wrap(stream.read(position, minOf(LARGE_BOX_HEADER.toLong(), stream.length - position).toInt()))
        val size32 = header.getInt(0).toLong() and UINT32_MAX
        val type = String(header.array(), 4, 4, Charsets.ISO_8859_1)
        val (size, headerSize) =
            when (size32) {
                0L -> stream.length - position to BOX_HEADER
                1L -> header.getLong(BOX_HEADER) to LARGE_BOX_HEADER
                else -> size32 to BOX_HEADER
            }
        if (size < headerSize || position + size > stream.length) throw IOException("Malformed $type box")
        if (type == "moov" || type == "moof") {
            val node = parse(type, stream.read(position + headerSize, (size - headerSize).toInt()))
            if (type == "moov") {
                val movieHeader = node.require("mvhd").payload
                tracks = node.children.orEmpty().filter { it.type == "trak" }.map { Track(stream, it, movieHeader) }
                defaults =
                    node.child("mvex")?.children.orEmpty().filter { it.type == "trex" }
                        .associate { ByteBuffer.wrap(it.payload).let { trex -> trex.getInt(4) to trex } }
            } else {
                val known = tracks ?: throw IOException("A fragment precedes the moov box")
                for (traf in node.children.orEmpty().filter { it.type == "traf" }) {
                    val id = ByteBuffer.wrap(traf.require("tfhd").payload).getInt(4)
                    val track = known.firstOrNull { it.id == id } ?: throw IOException("A fragment names no track $id")
                    track.readFragment(traf, position, defaults[id])
                }
            }
        }
        position += size
    }
    return tracks?.takeIf { it.isNotEmpty() } ?: throw IOException("A stream has no tracks")
}

// A full box's entries after its version, flags and entry count.
private fun <T> entries(payload: ByteArray, entrySize: Int, read: (ByteBuffer) -> T): List<T> {
    val buffer = ByteBuffer.wrap(payload, 8, payload.size - 8)
    val count = ByteBuffer.wrap(payload).getInt(4)
    if (count.toLong() * entrySize > payload.size - 8) throw IOException("Malformed table")
    return List(count) { read(buffer) }
}

// stts and ctts runs of (count, value), one value per sample; ctts values may be negative.
private fun expandRuns(payload: ByteArray): List<Long> =
    entries(payload, 8) { it.int to it.int.toLong() }.flatMap { (count, value) -> List(count) { value } }

internal fun writeMp4(
    streams: List<CachedStream>,
    output: OutputStream,
    tags: Tags,
    audioOnly: Boolean,
    onProgress: (Float) -> Unit,
) {
    val tracks = streams.flatMap(::tracks)
    val chunks = tracks.flatMap { it.chunks }.sortedBy { it.seconds }
    val dataSize = chunks.sumOf { it.size }
    val ftyp = serialize(Node("ftyp", fileType(audioOnly)))
    val mdatHeader = if (dataSize + BOX_HEADER > UINT32_MAX) LARGE_BOX_HEADER else BOX_HEADER
    val movieTimescale = timescale(tracks.first().movieHeader)

    fun movie(dataStart: Long, wide: Boolean): ByteArray {
        val offsets = HashMap<Chunk, Long>()
        chunks.fold(dataStart) { position, chunk -> position.also { offsets[chunk] = it } + chunk.size }
        val traks = tracks.mapIndexed { index, track -> trak(track, index + 1, movieTimescale, offsets, wide) }
        val header = movieHeader(tracks.first().movieHeader, traks.maxOf { it.second }, tracks.size + 1)
        val moov = container("moov", listOf(Node("mvhd", header)) + traks.map { it.first })
        addTags(moov, tags)
        return serialize(moov)
    }

    // Chunk offsets have a fixed width, so the movie box's size does not depend on them.
    val wide = ftyp.size + movie(0, false).size + mdatHeader + dataSize > UINT32_MAX
    val moov = movie(ftyp.size + movie(0, wide).size.toLong() + mdatHeader, wide)

    output.write(ftyp)
    output.write(moov)
    output.write(
        if (mdatHeader == LARGE_BOX_HEADER)
            ByteBuffer.allocate(LARGE_BOX_HEADER).putInt(1).put("mdat".toByteArray()).putLong(dataSize + LARGE_BOX_HEADER).array()
        else ByteBuffer.allocate(BOX_HEADER).putInt((dataSize + BOX_HEADER).toInt()).put("mdat".toByteArray()).array()
    )
    val buffer = ByteArray(COPY_BUFFER)
    var copied = 0L
    for (chunk in chunks) {
        chunk.track.stream.copyTo(output, chunk.position, chunk.size, buffer)
        copied += chunk.size
        onProgress(copied.toFloat() / dataSize)
    }
    output.flush()
}

// The track's boxes with its samples as tables, and its duration in the movie's timescale.
private fun trak(track: Track, id: Int, movieTimescale: Long, offsets: Map<Chunk, Long>, wide: Boolean): Pair<Node, Long> {
    val mediaDuration = track.duration
    val edit = track.trak.child("edts")?.let { editList(it.require("elst").payload, mediaDuration, track.timescale, movieTimescale) }
    val duration = edit?.second ?: (mediaDuration * movieTimescale / track.timescale)
    val mdia = track.trak.require("mdia")
    val minf = mdia.require("minf")
    val stbl = container("stbl", sampleTables(track, offsets, wide, minf.require("stbl").require("stsd")))
    val children =
        track.trak.children.orEmpty().map { child ->
            when (child.type) {
                "tkhd" -> Node("tkhd", trackHeader(child.payload, id, duration))
                "edts" -> container("edts", listOf(Node("elst", checkNotNull(edit).first)))
                "mdia" ->
                    container(
                        "mdia",
                        mdia.children.orEmpty().map { part ->
                            when (part.type) {
                                "mdhd" -> Node("mdhd", withDuration(part.payload, mediaDuration))
                                "minf" -> container("minf", minf.children.orEmpty().map { if (it.type == "stbl") stbl else it })
                                else -> part
                            }
                        },
                    )
                else -> child
            }
        }
    return container("trak", children) to duration
}

private fun sampleTables(track: Track, offsets: Map<Chunk, Long>, wide: Boolean, stsd: Node): List<Node> = buildList {
    val samples = track.samples
    add(stsd)
    add(Node("stts", runs(samples.map { it.duration })))
    if (samples.any { it.compositionOffset != 0 }) {
        val version = if (samples.any { it.compositionOffset < 0 }) 1 else 0
        add(Node("ctts", runs(samples.map { it.compositionOffset.toLong() }, version)))
    }
    if (samples.any { !it.sync }) {
        val sync = samples.indices.filter { samples[it].sync }
        add(Node("stss", table(sync.size, 4) { buffer -> sync.forEach { buffer.putInt(it + 1) } }))
    }
    add(Node("stsc", stsc(track.chunks)))
    // version and flags, a sample size of 0 (sizes vary), then the sample count and each size.
    val sizes = ByteBuffer.allocate(12 + samples.size * 4).putInt(0).putInt(0).putInt(samples.size)
    samples.forEach { sizes.putInt(it.size) }
    add(Node("stsz", sizes.array()))
    val chunkOffsets = track.chunks.map(offsets::getValue)
    add(
        Node(
            if (wide) "co64" else "stco",
            table(chunkOffsets.size, if (wide) 8 else 4) { buffer ->
                chunkOffsets.forEach { if (wide) buffer.putLong(it) else buffer.putInt(it.toInt()) }
            },
        )
    )
}

// A full box's table: version and flags, entry count, then the entries.
private fun table(count: Int, entrySize: Int, version: Int = 0, entries: (ByteBuffer) -> Unit): ByteArray =
    ByteBuffer.allocate(8 + count * entrySize).putInt(version shl 24).putInt(count).also(entries).array()

private fun runs(values: List<Long>, version: Int = 0): ByteArray {
    val grouped = mutableListOf<Pair<Int, Long>>()
    for (value in values) {
        val last = grouped.lastOrNull()
        if (last != null && last.second == value) grouped[grouped.size - 1] = last.first + 1 to value else grouped += 1 to value
    }
    return table(grouped.size, 8, version) { buffer -> grouped.forEach { (count, value) -> buffer.putInt(count).putInt(value.toInt()) } }
}

// One entry per run of chunks with the same sample count: first chunk (from 1), samples per chunk, description 1.
private fun stsc(chunks: List<Chunk>): ByteArray {
    val entries = mutableListOf<Pair<Int, Int>>()
    chunks.forEachIndexed { index, chunk -> if (entries.lastOrNull()?.second != chunk.samples) entries += index + 1 to chunk.samples }
    return table(entries.size, 12) { buffer -> entries.forEach { (first, samples) -> buffer.putInt(first).putInt(samples).putInt(1) } }
}

/**
 * The edit list with any open-ended entry (duration 0, as fragmented files leave it) closed at the end of the media,
 * and the edits' total duration in the movie's timescale.
 */
private fun editList(elst: ByteArray, mediaDuration: Long, mediaTimescale: Long, movieTimescale: Long): Pair<ByteArray, Long> {
    val input = ByteBuffer.wrap(elst)
    val wide = input.get(0).toInt() == 1
    input.position(4)
    val count = input.int
    val output = ByteBuffer.allocate(elst.size).put(elst, 0, 8)
    var total = 0L
    repeat(count) {
        var segment = if (wide) input.long else input.int.toLong() and UINT32_MAX
        val mediaTime = if (wide) input.long else input.int.toLong()
        val rate = input.int
        if (segment == 0L) segment = (mediaDuration - maxOf(mediaTime, 0)) * movieTimescale / mediaTimescale
        total += segment
        if (wide) output.putLong(segment).putLong(mediaTime) else output.putInt(segment.toInt()).putInt(mediaTime.toInt())
        output.putInt(rate)
    }
    return output.array() to total
}

// mvhd and mdhd: version and flags, creation and modification times, timescale, duration.
private fun timescale(header: ByteArray): Long =
    ByteBuffer.wrap(header).getInt(if (header[0].toInt() == 1) 20 else 12).toLong() and UINT32_MAX

private fun withDuration(header: ByteArray, duration: Long): ByteArray =
    header.copyOf().also {
        val buffer = ByteBuffer.wrap(it)
        if (it[0].toInt() == 1) buffer.putLong(24, duration) else buffer.putInt(16, duration.toInt())
    }

// mvhd ends with the next track ID.
private fun movieHeader(header: ByteArray, duration: Long, nextTrackId: Int): ByteArray =
    withDuration(header, duration).also { ByteBuffer.wrap(it).putInt(it.size - 4, nextTrackId) }

// tkhd: version and flags, creation and modification times, track ID, reserved, duration.
private fun trackHeader(header: ByteArray, id: Int, duration: Long): ByteArray =
    header.copyOf().also {
        val buffer = ByteBuffer.wrap(it)
        if (it[0].toInt() == 1) buffer.putInt(20, id).putLong(28, duration) else buffer.putInt(12, id).putInt(20, duration.toInt())
    }

private fun fileType(audioOnly: Boolean): ByteArray {
    val brands = if (audioOnly) listOf("M4A ", "isom", "iso2", "mp41") else listOf("isom", "iso2", "mp41")
    return ByteBuffer.allocate(8 + brands.size * 4)
        .put(brands.first().toByteArray())
        .putInt(0x200)
        .apply { brands.forEach { put(it.toByteArray()) } }
        .array()
}
