// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

/**
 * Incremental UMP control observer. Headers and payloads may span any number of response chunks.
 * Media is skipped in place; only bounded control payloads are copied. The caller's buffer position,
 * limit and contents are untouched, so native code remains the sole consumer of media data.
 */
final class SabrResponse {
    static final int RELOAD_PLAYER_RESPONSE = 46;
    static final int CONTEXT_UPDATE = 57;
    static final int STREAM_PROTECTION_STATUS = 58;
    static final int CONTEXT_SENDING_POLICY = 59;
    private static final int MAX_CONTROL_BYTES = 64 * 1024;
    private final BiConsumer<Integer, byte[]> controls;
    private final byte[] header = new byte[5];
    private int headerSize;
    private long type = -1;
    private long remaining = -1;
    private byte[] payload;
    private int written;

    SabrResponse(BiConsumer<Integer, byte[]> controls) {
        this.controls = controls;
    }

    synchronized void read(ByteBuffer original) {
        ByteBuffer buffer = original.duplicate();
        while (buffer.hasRemaining()) {
            if (type < 0) {
                type = integer(buffer);
                if (type < 0) return;
            }
            if (remaining < 0) {
                remaining = integer(buffer);
                if (remaining < 0) return;
                if (type == RELOAD_PLAYER_RESPONSE || type == CONTEXT_UPDATE
                        || type == STREAM_PROTECTION_STATUS || type == CONTEXT_SENDING_POLICY) {
                    if (remaining > MAX_CONTROL_BYTES) throw new IllegalArgumentException("SABR control exceeds size limit");
                    payload = new byte[(int) remaining];
                }
            }
            int count = (int) Math.min(remaining, buffer.remaining());
            if (payload != null) {
                buffer.get(payload, written, count);
                written += count;
            } else buffer.position(buffer.position() + count);
            remaining -= count;
            if (remaining == 0) {
                if (payload != null) controls.accept((int) type, payload);
                type = -1;
                remaining = -1;
                payload = null;
                written = 0;
            }
        }
    }

    /** UMP's prefix-coded unsigned integer, not protobuf's continuation-bit varint. */
    private long integer(ByteBuffer buffer) {
        if (headerSize == 0) {
            if (!buffer.hasRemaining()) return -1;
            header[headerSize++] = buffer.get();
        }
        int first = header[0] & 0xFF;
        int length = first < 128 ? 1 : first < 192 ? 2 : first < 224 ? 3 : first < 240 ? 4 : 5;
        while (headerSize < length && buffer.hasRemaining()) header[headerSize++] = buffer.get();
        if (headerSize < length) return -1;
        long value = length == 5 ? 0 : first & ((1 << (8 - length)) - 1);
        for (int index = 1, shift = length == 5 ? 0 : 8 - length; index < length; index++, shift += 8) {
            value |= (long) (header[index] & 0xFF) << shift;
        }
        headerSize = 0;
        return value;
    }
}
