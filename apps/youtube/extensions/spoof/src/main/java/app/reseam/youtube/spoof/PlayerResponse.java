// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import app.reseam.youtube.spoof.Proto.Field;

import static app.reseam.youtube.spoof.Proto.WIRE_LENGTH;
import static app.reseam.youtube.spoof.Proto.WIRE_VARINT;
import static app.reseam.youtube.spoof.Proto.bytes;
import static app.reseam.youtube.spoof.Proto.fields;
import static app.reseam.youtube.spoof.Proto.writeBytes;

/**
 * The parts of an InnerTube player response the spoof request reads, decoded from protobuf
 * wire format. Field numbers follow YouTube's messages:
 * PlayerResponse { PlayabilityStatus playability_status = 2; StreamingData streaming_data = 4; },
 * PlayabilityStatus { Status status = 1; string reason = 2; },
 * StreamingData { repeated Format formats = 2; repeated Format adaptiveFormats = 3; },
 * Format { string url = 2; string mimeType = 5; string qualityLabel = 26; string signatureCipher = 48; }.
 */
final class PlayerResponse {
    private static final int PLAYABILITY_STATUS = 2;
    private static final int STREAMING_DATA = 4;
    private static final int PLAYER_CONFIG = 15;
    /** MediaCommonConfig's extension number in PlayerConfig. */
    private static final int MEDIA_COMMON_CONFIG = 215771584;
    private static final int STATUS = 1;
    private static final int REASON = 2;
    private static final int FORMATS = 2;
    private static final int ADAPTIVE_FORMATS = 3;
    private static final int URL = 2;
    private static final int MIME_TYPE = 5;
    private static final int QUALITY_LABEL = 26;
    private static final int SIGNATURE_CIPHER = 48;

    /** PlayabilityStatus.Status.OK, also the value of an absent status. */
    static final int STATUS_OK = 0;

    final int status;
    final String reason;
    /** The encoded StreamingData message, or null when the response has none. */
    final byte[] streamingData;
    /** The encoded MediaCommonConfig extension, or null when the response has none. */
    final byte[] mediaCommonConfig;

    private PlayerResponse(int status, String reason, byte[] streamingData, byte[] mediaCommonConfig) {
        this.status = status;
        this.reason = reason;
        this.streamingData = streamingData;
        this.mediaCommonConfig = mediaCommonConfig;
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
        byte[] config = bytes(playerResponse, PLAYER_CONFIG);
        byte[] common = config == null ? null : bytes(config, MEDIA_COMMON_CONFIG);
        return new PlayerResponse(status, reason, streamingData, common);
    }

    /** Direct adaptive playback requires both an audio track and a video track with resolved URLs. */
    static boolean hasAdaptiveStreams(byte[] streamingData) {
        boolean audio = false;
        boolean video = false;
        for (Field field : fields(streamingData)) {
            if (field.number != ADAPTIVE_FORMATS || field.wireType != WIRE_LENGTH) continue;
            StreamUrl url = streamUrl(field.bytes);
            if (url == null || url.url == null || url.url.isEmpty()) continue;
            String mime = Format.parse(field.bytes).mimeType;
            audio |= mime.startsWith("audio/");
            video |= mime.startsWith("video/");
        }
        return audio && video;
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

    /** Where a format streams from: a plain URL, or a signature cipher (`s`, `sp`, `url` query) when null. */
    record StreamUrl(String url, String signatureCipher) {}

    /** The stream location of every muxed and adaptive format that has one. */
    static List<StreamUrl> streamUrls(byte[] streamingData) {
        List<StreamUrl> urls = new ArrayList<>();
        for (Field field : fields(streamingData)) {
            if (!isFormat(field)) continue;
            StreamUrl url = streamUrl(field.bytes);
            if (url != null) urls.add(url);
        }
        return urls;
    }

    /**
     * `streamingData` with each format's stream location replaced by the plain URL `resolve` returns;
     * formats without one, including those for which the resolver returns null, are dropped.
     */
    static byte[] withResolvedUrls(byte[] streamingData, Function<StreamUrl, String> resolve) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(streamingData.length);
        for (Field field : fields(streamingData)) {
            if (!isFormat(field)) {
                output.write(streamingData, field.start, field.end - field.start);
                continue;
            }
            StreamUrl url = streamUrl(field.bytes);
            if (url == null) continue;
            String resolved = resolve.apply(url);
            if (resolved == null) continue;
            ByteArrayOutputStream format = new ByteArrayOutputStream(field.bytes.length);
            for (Field item : fields(field.bytes)) {
                boolean location = item.wireType == WIRE_LENGTH && (item.number == URL || item.number == SIGNATURE_CIPHER);
                if (!location) format.write(field.bytes, item.start, item.end - item.start);
            }
            writeBytes(format, URL, resolved.getBytes(StandardCharsets.UTF_8));
            writeBytes(output, field.number, format.toByteArray());
        }
        return output.toByteArray();
    }

    private static StreamUrl streamUrl(byte[] format) {
        String cipher = null;
        for (Field item : fields(format)) {
            if (item.wireType != WIRE_LENGTH) continue;
            if (item.number == URL && !item.string().isEmpty()) return new StreamUrl(item.string(), null);
            if (item.number == SIGNATURE_CIPHER && !item.string().isEmpty()) cipher = item.string();
        }
        return cipher == null ? null : new StreamUrl(null, cipher);
    }

    private static boolean isFormat(Field field) {
        return (field.number == FORMATS || field.number == ADAPTIVE_FORMATS) && field.wireType == WIRE_LENGTH;
    }

    /** A PlayerResponse holding only `streamingData`, as the app's parser reads it. */
    static byte[] withStreamingData(byte[] streamingData) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(streamingData.length + 6);
        writeBytes(output, STREAMING_DATA, streamingData);
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
}
