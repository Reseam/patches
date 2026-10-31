// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.core

import app.reseam.youtube.core.Proto.Field
import app.reseam.youtube.core.Proto.WIRE_END_GROUP
import app.reseam.youtube.core.Proto.WIRE_FIXED32
import app.reseam.youtube.core.Proto.WIRE_FIXED64
import app.reseam.youtube.core.Proto.WIRE_LENGTH
import app.reseam.youtube.core.Proto.WIRE_START_GROUP
import app.reseam.youtube.core.Proto.WIRE_VARINT
import java.io.ByteArrayOutputStream

internal class ProtoReader(private val message: ByteArray) {
    private var position = 0

    val hasMore: Boolean
        get() = position < message.size

    fun field(): Field {
        val start = position
        val (number, wireType) = tag()
        require(wireType != WIRE_END_GROUP) { "Unbalanced protobuf group" }
        var varint = 0L
        val bytes =
            if (wireType == WIRE_VARINT) {
                varint = varint()
                null
            } else value(number, wireType)
        return Field(number, wireType, varint, bytes, start, position)
    }

    private fun tag(): Pair<Int, Int> {
        val tag = varint()
        val number = tag ushr 3
        require(number in 1..0x1FFFFFFFL) { "Invalid protobuf tag" }
        return number.toInt() to (tag and 7).toInt()
    }

    private fun value(number: Int, wireType: Int): ByteArray =
        when (wireType) {
            WIRE_FIXED64 -> take(8L)
            WIRE_LENGTH -> lengthDelimited()
            WIRE_START_GROUP -> group(number)
            WIRE_FIXED32 -> take(4L)
            else -> throw IllegalArgumentException("Unsupported protobuf wire type $wireType")
        }

    private fun group(number: Int): ByteArray {
        val start = position
        while (true) {
            val end = position
            val (field, wireType) = tag()
            if (wireType == WIRE_END_GROUP) {
                require(field == number) { "Unbalanced protobuf group" }
                return message.copyOfRange(start, end)
            }
            if (wireType == WIRE_VARINT) varint() else value(field, wireType)
        }
    }

    fun varint(): Long {
        var value = 0L
        for (shift in 0 until 64 step 7) {
            require(hasMore) { "Truncated protobuf varint" }
            val byte = message[position++].toInt()
            require(shift != 63 || (byte and 0xFE) == 0) { "Malformed protobuf varint" }
            value = value or ((byte and 0x7F).toLong() shl shift)
            if (byte >= 0) return value
        }
        throw IllegalArgumentException("Malformed protobuf varint")
    }

    private fun lengthDelimited(): ByteArray = take(varint())

    private fun take(count: Long): ByteArray {
        require(count in 0..message.size - position) { "Truncated protobuf field" }
        return message.copyOfRange(position, position + count.toInt()).also { position += it.size }
    }
}

internal fun ByteArrayOutputStream.writeNumber(number: Int, value: Long) {
    writeVarint(number.toLong() shl 3)
    writeVarint(value)
}

internal fun ByteArrayOutputStream.writeBytes(number: Int, bytes: ByteArray) {
    writeVarint((number.toLong() shl 3) or WIRE_LENGTH.toLong())
    writeVarint(bytes.size.toLong())
    write(bytes, 0, bytes.size)
}

internal fun ByteArrayOutputStream.writeVarint(value: Long) {
    var remaining = value
    while ((remaining and 0x7FL.inv()) != 0L) {
        write(((remaining and 0x7F) or 0x80).toInt())
        remaining = remaining ushr 7
    }
    write(remaining.toInt())
}
