// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import app.reseam.youtube.core.Proto

// Android owns PLAYBACK contexts; these CONTENT_ADS contexts belong to one native playback.
internal class SabrContexts {
    private data class Context(val value: ByteArray, val active: Boolean)

    private enum class Policy(val field: Int) {
        START(1),
        STOP(2),
        DISCARD(3),
    }

    private val contexts = linkedMapOf<Int, Context>()

    @Synchronized
    fun update(message: ByteArray) {
        // SabrContextUpdate: type (1), scope (2), value (3), sendByDefault (4), writePolicy (5).
        if (Proto.number(message, 2, 0).toInt() != 4) return
        val type = Proto.number(message, 1, 0).toInt()
        val value = Proto.bytes(message, 3)
        require(type > 0 && value != null) { "Invalid SABR context update" }
        if (Proto.number(message, 5, 0) == 2L && type in contexts) return
        val bytes =
            contexts.entries.sumOf { (key, context) ->
                if (key == type) 0 else context.value.size
            } + value.size
        require(bytes <= 1024 * 1024 && (type in contexts || contexts.size < 256)) {
            "SABR context storage limit exceeded"
        }
        contexts[type] = Context(value, Proto.number(message, 4, 0) != 0L)
    }

    @Synchronized
    fun policy(message: ByteArray) {
        // Decode every packed list before applying start (1), stop (2) and discard (3).
        val operations =
            Proto.fields(message)
                .filter { it.number in 1..3 }
                .groupBy { it.number }
                .mapValues { (_, fields) -> fields.flatMap(::types).toSet() }
        Policy.entries.forEach { policy ->
            operations[policy.field].orEmpty().forEach { type ->
                contexts[type]?.let { context ->
                    when (policy) {
                        Policy.START -> contexts[type] = context.copy(active = true)
                        Policy.STOP -> contexts[type] = context.copy(active = false)
                        Policy.DISCARD -> contexts.remove(type)
                    }
                }
            }
        }
    }

    @Synchronized
    fun append(streamerContext: ByteArray): ByteArray {
        // StreamerContext: sabrContexts (5), unsentSabrContexts (6); context: type (1), value (2).
        val nativeTypes =
            Proto.fields(streamerContext)
                .flatMap { field ->
                    when {
                        field.number == 5 && field.wireType == Proto.WIRE_LENGTH ->
                            listOf(Proto.number(checkNotNull(field.bytes), 1, 0).toInt())
                        field.number == 6 -> types(field)
                        else -> emptyList()
                    }
                }
                .toSet()
        val appended = Proto.Writer()
        for ((type, context) in contexts.filterKeys { it !in nativeTypes }) {
            if (context.active)
                appended.message(5, Proto.Writer().number(1, type.toLong()).bytes(2, context.value))
            else appended.number(6, type.toLong())
        }
        return streamerContext + appended.toByteArray()
    }

    private fun types(field: Proto.Field): List<Int> =
        when (field.wireType) {
            Proto.WIRE_VARINT -> listOf(field.varint.toInt())
            Proto.WIRE_LENGTH -> Proto.varints(checkNotNull(field.bytes)).map { it.toInt() }
            else -> emptyList()
        }
}
