// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.core

import java.io.ByteArrayOutputStream

/** Bounded protobuf operations that preserve unknown fields byte for byte during rewrites. */
object Proto {
    const val WIRE_VARINT = 0
    const val WIRE_FIXED64 = 1
    const val WIRE_LENGTH = 2
    const val WIRE_START_GROUP = 3
    const val WIRE_END_GROUP = 4
    const val WIRE_FIXED32 = 5

    class Field
    internal constructor(
        val number: Int,
        val wireType: Int,
        val varint: Long,
        val bytes: ByteArray?,
        /** Encoded span in the message, including the tag. */
        val start: Int,
        val end: Int,
    ) {
        fun string(): String = checkNotNull(bytes).decodeToString()
    }

    /**
     * Decodes fields in wire order; a group's bytes are its fields. Rejects malformed tags,
     * unbalanced groups and truncation.
     */
    fun fields(message: ByteArray): List<Field> =
        ProtoReader(message).run { buildList { while (hasMore) add(field()) } }

    fun varints(packed: ByteArray): List<Long> =
        ProtoReader(packed).run { buildList { while (hasMore) add(varint()) } }

    fun bytes(message: ByteArray, number: Int): ByteArray? = fields(message).bytes(number)

    fun string(message: ByteArray, number: Int): String? = bytes(message, number)?.decodeToString()

    fun number(message: ByteArray, number: Int, fallback: Long): Long =
        fields(message).number(number) ?: fallback

    /**
     * Removes every occurrence of the given fields, then appends the non-null replacements in
     * order.
     */
    fun replace(message: ByteArray, replacements: Map<Int, ByteArray?>): ByteArray =
        ByteArrayOutputStream(message.size)
            .apply {
                fields(message)
                    .filterNot { it.number in replacements }
                    .forEach { write(message, it.start, it.end - it.start) }
                replacements.forEach { (number, value) ->
                    if (value != null) writeBytes(number, value)
                }
            }
            .toByteArray()

    /**
     * Null removes a length-delimited field; returning the same array keeps its original encoding.
     */
    fun rewriteLength(
        message: ByteArray,
        rewrite: (number: Int, value: ByteArray) -> ByteArray?,
    ): ByteArray =
        rewriteFields(message) { field ->
            val value =
                if (field.wireType == WIRE_LENGTH) rewrite(field.number, checkNotNull(field.bytes))
                else field.bytes
            if (value === field.bytes) null
            else {
                { value?.let { writeBytes(field.number, it) } }
            }
        }

    fun rewriteNumber(message: ByteArray, number: Int, rewrite: (Long) -> Long): ByteArray =
        rewriteFields(message) { field ->
            val value =
                if (field.number == number && field.wireType == WIRE_VARINT) rewrite(field.varint)
                else field.varint
            if (value == field.varint) null
            else {
                { writeNumber(field.number, value) }
            }
        }

    private inline fun rewriteFields(
        message: ByteArray,
        edit: (Field) -> (ByteArrayOutputStream.() -> Unit)?,
    ): ByteArray {
        var output: ByteArrayOutputStream? = null
        for (field in fields(message)) {
            val change = edit(field)
            if (output == null && change != null)
                output =
                    ByteArrayOutputStream(message.size).apply { write(message, 0, field.start) }
            val destination = output ?: continue
            if (change != null) destination.change()
            else destination.write(message, field.start, field.end - field.start)
        }
        return output?.toByteArray() ?: message
    }

    class Writer {
        private val output = ByteArrayOutputStream()

        fun number(number: Int, value: Long): Writer = apply { output.writeNumber(number, value) }

        fun bytes(number: Int, value: ByteArray): Writer = apply {
            output.writeBytes(number, value)
        }

        fun string(number: Int, value: String): Writer = bytes(number, value.encodeToByteArray())

        fun message(number: Int, message: Writer): Writer = bytes(number, message.toByteArray())

        fun toByteArray(): ByteArray = output.toByteArray()
    }
}

/** The last length-delimited occurrence of [number]. */
fun List<Proto.Field>.bytes(number: Int): ByteArray? = lastOrNull {
    it.number == number && it.wireType == Proto.WIRE_LENGTH
}
    ?.bytes

fun List<Proto.Field>.string(number: Int): String? = bytes(number)?.decodeToString()

/** The last varint occurrence of [number]. */
fun List<Proto.Field>.number(number: Int): Long? = lastOrNull {
    it.number == number && it.wireType == Proto.WIRE_VARINT
}
    ?.varint
