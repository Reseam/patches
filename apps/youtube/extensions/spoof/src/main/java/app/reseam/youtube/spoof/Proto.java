// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Bounded protobuf wire operations. Unknown fields are preserved byte-for-byte when replacing fields. */
final class Proto {
    static final int WIRE_VARINT = 0;
    static final int WIRE_FIXED64 = 1;
    static final int WIRE_LENGTH = 2;
    static final int WIRE_FIXED32 = 5;

    private Proto() {}

    static byte[] bytes(byte[] message, int number) {
        byte[] value = null;
        for (Field field : fields(message)) {
            if (field.number == number && field.wireType == WIRE_LENGTH) value = field.bytes;
        }
        return value;
    }

    static long number(byte[] message, int number, long fallback) {
        long value = fallback;
        for (Field field : fields(message)) {
            if (field.number == number && field.wireType == WIRE_VARINT) value = field.varint;
        }
        return value;
    }

    static byte[] replace(byte[] message, Map<Integer, byte[]> replacements) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(message.length);
        for (Field field : fields(message)) {
            if (!replacements.containsKey(field.number)) output.write(message, field.start, field.end - field.start);
        }
        replacements.forEach((number, value) -> {
            if (value != null) writeBytes(output, number, value);
        });
        return output.toByteArray();
    }

    static void writeNumber(ByteArrayOutputStream output, int number, long value) {
        writeVarint(output, (long) number << 3);
        writeVarint(output, value);
    }

    static final class Field {
        final int number;
        final int wireType;
        final long varint;
        final byte[] bytes;
        /** The field's encoded span in its message, tag included. */
        final int start;
        final int end;

        Field(int number, int wireType, long varint, byte[] bytes, int start, int end) {
            this.number = number;
            this.wireType = wireType;
            this.varint = varint;
            this.bytes = bytes;
            this.start = start;
            this.end = end;
        }

        String string() {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    static List<Field> fields(byte[] message) {
        List<Field> fields = new ArrayList<>();
        int[] position = {0};
        while (position[0] < message.length) {
            int start = position[0];
            long tag = readVarint(message, position);
            if (tag <= 0 || (tag >>> 3) > 0x1FFFFFFFL || (tag >>> 3) == 0) {
                throw new IllegalArgumentException("Invalid protobuf tag");
            }
            int number = (int) (tag >>> 3);
            int wireType = (int) (tag & 7);
            long varint = 0;
            byte[] bytes = null;
            switch (wireType) {
                case WIRE_VARINT:
                    varint = readVarint(message, position);
                    break;
                case WIRE_FIXED64:
                    position[0] += 8;
                    break;
                case WIRE_LENGTH:
                    long encodedLength = readVarint(message, position);
                    if (encodedLength < 0 || encodedLength > message.length - position[0]) {
                        throw new IllegalArgumentException("Truncated protobuf field " + number);
                    }
                    int length = (int) encodedLength;
                    bytes = new byte[length];
                    System.arraycopy(message, position[0], bytes, 0, length);
                    position[0] += length;
                    break;
                case WIRE_FIXED32:
                    position[0] += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported protobuf wire type " + wireType);
            }
            if (position[0] > message.length) throw new IllegalArgumentException("Truncated protobuf message");
            fields.add(new Field(number, wireType, varint, bytes, start, position[0]));
        }
        return fields;
    }

    static long readVarint(byte[] message, int[] position) {
        long value = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            if (position[0] >= message.length) throw new IllegalArgumentException("Truncated protobuf varint");
            byte b = message[position[0]++];
            if (shift == 63 && (b & 0xFE) != 0) throw new IllegalArgumentException("Malformed protobuf varint");
            value |= (long) (b & 0x7F) << shift;
            if (b >= 0) return value;
        }
        throw new IllegalArgumentException("Malformed protobuf varint");
    }

    static void writeBytes(ByteArrayOutputStream output, int number, byte[] bytes) {
        writeVarint(output, ((long) number << 3) | WIRE_LENGTH);
        writeVarint(output, bytes.length);
        output.write(bytes, 0, bytes.length);
    }

    static void writeVarint(ByteArrayOutputStream output, long value) {
        while ((value & ~0x7FL) != 0) {
            output.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        output.write((int) value);
    }
}
