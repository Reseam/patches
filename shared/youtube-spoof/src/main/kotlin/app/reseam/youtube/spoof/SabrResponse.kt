// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import java.nio.ByteBuffer

internal const val RELOAD_PLAYER_RESPONSE = 46
internal const val CONTEXT_UPDATE = 57
internal const val STREAM_PROTECTION_STATUS = 58
internal const val CONTEXT_SENDING_POLICY = 59
private val controlTypes =
    setOf(
        RELOAD_PLAYER_RESPONSE.toLong(),
        CONTEXT_UPDATE.toLong(),
        STREAM_PROTECTION_STATUS.toLong(),
        CONTEXT_SENDING_POLICY.toLong(),
    )

// Only control payloads are copied; media and the caller's buffer stay untouched.
internal class SabrResponse(private val controls: (Int, ByteArray) -> Unit) {
    private sealed interface State {
        data object Type : State

        data class Length(val type: Long) : State

        class Payload(val type: Long, var remaining: Long, val bytes: ByteArray?) : State
    }

    private var state: State = State.Type
    private val header = ByteArray(5)
    private var headerSize = 0

    @Synchronized
    fun read(original: ByteBuffer) {
        val buffer = original.duplicate()
        while (buffer.hasRemaining()) {
            state =
                when (val current = state) {
                    State.Type -> State.Length(integer(buffer) ?: return)
                    is State.Length -> {
                        val size = integer(buffer) ?: return
                        val bytes =
                            if (current.type in controlTypes) {
                                require(size <= 64 * 1024) { "SABR control exceeds size limit" }
                                ByteArray(size.toInt())
                            } else null
                        // Zero-length controls complete even when the header consumed the last
                        // byte.
                        val payload = State.Payload(current.type, size, bytes)
                        state = payload
                        consume(payload, buffer)
                    }
                    is State.Payload -> consume(current, buffer)
                }
        }
    }

    private fun consume(payload: State.Payload, buffer: ByteBuffer): State {
        val count = minOf(payload.remaining, buffer.remaining().toLong()).toInt()
        payload.bytes?.let { buffer.get(it, it.size - payload.remaining.toInt(), count) }
            ?: buffer.position(buffer.position() + count)
        payload.remaining -= count
        if (payload.remaining != 0L) return payload
        payload.bytes?.let { controls(payload.type.toInt(), it) }
        return State.Type
    }

    // UMP uses prefix-coded unsigned integers rather than protobuf continuation bits.
    private fun integer(buffer: ByteBuffer): Long? {
        if (headerSize == 0 && buffer.hasRemaining()) header[headerSize++] = buffer.get()
        if (headerSize == 0) return null
        val first = header[0].toInt() and 0xFF
        val length =
            when {
                first < 128 -> 1
                first < 192 -> 2
                first < 224 -> 3
                first < 240 -> 4
                else -> 5
            }
        while (headerSize < length && buffer.hasRemaining()) header[headerSize++] = buffer.get()
        if (headerSize < length) return null
        val initial = if (length == 5) 0L else (first and ((1 shl (8 - length)) - 1)).toLong()
        val shift = if (length == 5) 0 else 8 - length
        headerSize = 0
        return (1 until length).fold(initial) { value, index ->
            value or ((header[index].toLong() and 0xFF) shl (shift + (index - 1) * 8))
        }
    }
}
