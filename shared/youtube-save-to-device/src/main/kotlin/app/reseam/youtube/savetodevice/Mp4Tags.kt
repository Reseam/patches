// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.savetodevice

import java.nio.ByteBuffer

internal class Tags(
    val title: String,
    val artist: String?,
    val album: String?,
    val track: Pair<Int, Int>?,
    val cover: Cover?,
)

internal class Cover(val bytes: ByteArray, val png: Boolean)

// iTunes metadata well-known data types.
private const val DATA_IMPLICIT = 0
private const val DATA_UTF8 = 1
private const val DATA_JPEG = 13
private const val DATA_PNG = 14

internal fun addTags(moov: Node, tags: Tags) {
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
    val meta = Node("meta", ByteArray(4) + serialize(Node("hdlr", handler.array())) + serialize(container("ilst", items)))
    val moovChildren = checkNotNull(moov.children)
    moovChildren.removeAll { it.type == "udta" }
    moovChildren += container("udta", listOf(meta))
}

private fun item(type: String, dataType: Int, value: ByteArray): Node {
    val data = ByteBuffer.allocate(8 + value.size).putInt(dataType).putInt(0).put(value)
    return container(type, listOf(Node("data", data.array())))
}
