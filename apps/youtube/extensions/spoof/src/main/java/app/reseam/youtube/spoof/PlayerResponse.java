// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The parts of an InnerTube player response the spoof request reads, decoded from protobuf
 * wire format. Field numbers follow YouTube's messages:
 * PlayerResponse { PlayabilityStatus playability_status = 2; StreamingData streaming_data = 4; },
 * PlayabilityStatus { Status status = 1; string reason = 2; },
 * StreamingData { repeated Format adaptiveFormats = 3; },
 * Format { string mimeType = 5; string qualityLabel = 26; },
 * ReelItemWatchResponse { PlayerResponse player_response = 4; }.
 */
final class PlayerResponse {
    private static final int WIRE_VARINT = 0;
    private static final int WIRE_FIXED64 = 1;
    private static final int WIRE_LENGTH = 2;
    private static final int WIRE_FIXED32 = 5;

    private static final int PLAYABILITY_STATUS = 2;
    private static final int STREAMING_DATA = 4;
    private static final int STATUS = 1;
    private static final int REASON = 2;
    private static final int ADAPTIVE_FORMATS = 3;
    private static final int MIME_TYPE = 5;
    private static final int QUALITY_LABEL = 26;
    private static final int REEL_PLAYER_RESPONSE = 4;

    /** PlayabilityStatus.Status.OK, also the value of an absent status. */
    static final int STATUS_OK = 0;

    final int status;
    final String reason;
    /** The encoded StreamingData message, or null when the response has none. */
    final byte[] streamingData;

    private PlayerResponse(int status, String reason, byte[] streamingData) {
        this.status = status;
        this.reason = reason;
        this.streamingData = streamingData;
    }

    static PlayerResponse parse(byte[] playerResponse) {
        int status = STATUS_OK;
        String reason = null;
        byte[] streamingData = null;
        for (Field field : fields(playerResponse)) {
            if (field.number == PLAYABILITY_STATUS && field.wireType == WIRE_LENGTH) {
                for (Field item : fields(field.bytes)) {
                    if (item.number == STATUS && item.wireType == WIRE_VARINT) status = (int) item.varint;
                    else if (item.number == REASON && item.wireType == WIRE_LENGTH) reason = item.string();
                }
            } else if (field.number == STREAMING_DATA && field.wireType == WIRE_LENGTH) {
                streamingData = field.bytes;
            }
        }
        return new PlayerResponse(status, reason, streamingData);
    }

    /** The player response a reel item watch response wraps, or null. */
    static byte[] unwrapReel(byte[] reelItemWatchResponse) {
        byte[] playerResponse = null;
        for (Field field : fields(reelItemWatchResponse)) {
            if (field.number == REEL_PLAYER_RESPONSE && field.wireType == WIRE_LENGTH) playerResponse = field.bytes;
        }
        return playerResponse;
    }

    static int adaptiveFormatCount(byte[] streamingData) {
        int count = 0;
        for (Field field : fields(streamingData)) {
            if (field.number == ADAPTIVE_FORMATS && field.wireType == WIRE_LENGTH) count++;
        }
        return count;
    }

    /**
     * YouTube picks one codec family before building its quality list. When a response has a
     * single VP9 quality and a longer AVC ladder, drop VP9 so the picker offers every AVC quality.
     */
    static byte[] preferMultipleAvcQualities(byte[] streamingData) {
        Set<String> avc = new HashSet<>();
        Set<String> vp9 = new HashSet<>();
        List<Field> fields = fields(streamingData);
        for (Field field : fields) {
            if (field.number != ADAPTIVE_FORMATS || field.wireType != WIRE_LENGTH) continue;
            Format format = Format.parse(field.bytes);
            if (format.qualityLabel == null || format.qualityLabel.isEmpty()) continue;
            if (format.mimeType.contains("avc1")) avc.add(format.qualityLabel);
            else if (format.mimeType.contains("vp9")) vp9.add(format.qualityLabel);
        }
        if (vp9.size() != 1 || avc.size() <= vp9.size()) return streamingData;

        ByteArrayOutputStream output = new ByteArrayOutputStream(streamingData.length);
        for (Field field : fields) {
            if (field.number == ADAPTIVE_FORMATS && field.wireType == WIRE_LENGTH && Format.parse(field.bytes).mimeType.contains("vp9")) continue;
            output.write(streamingData, field.start, field.end - field.start);
        }
        return output.toByteArray();
    }

    /** A PlayerResponse holding only `streamingData`, as the app's parser reads it. */
    static byte[] withStreamingData(byte[] streamingData) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(streamingData.length + 6);
        writeVarint(output, ((long) STREAMING_DATA << 3) | WIRE_LENGTH);
        writeVarint(output, streamingData.length);
        output.write(streamingData, 0, streamingData.length);
        return output.toByteArray();
    }

    private static final class Format {
        final String mimeType;
        final String qualityLabel;

        private Format(String mimeType, String qualityLabel) {
            this.mimeType = mimeType;
            this.qualityLabel = qualityLabel;
        }

        static Format parse(byte[] format) {
            String mimeType = "";
            String qualityLabel = null;
            for (Field field : fields(format)) {
                if (field.number == MIME_TYPE && field.wireType == WIRE_LENGTH) mimeType = field.string();
                else if (field.number == QUALITY_LABEL && field.wireType == WIRE_LENGTH) qualityLabel = field.string();
            }
            return new Format(mimeType, qualityLabel);
        }
    }

    private static final class Field {
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
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private static List<Field> fields(byte[] message) {
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

    private static long readVarint(byte[] message, int[] position) {
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

    private static void writeVarint(ByteArrayOutputStream output, long value) {
        while ((value & ~0x7FL) != 0) {
            output.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        output.write((int) value);
    }
}
