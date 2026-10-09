// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.net.Uri;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import app.reseam.youtube.web.WebPlayer;

/** One validated WEB response: native formats, solved SABR URL, decoder configuration and attestation. */
record SabrData(String videoId, byte[] streams, Object nativeCommonConfig, Uri uri,
               WebPlayer.Attestation attestation) {
    /** Validates the format pair and native config, then solves the SABR URL or throws on failure. */
    static SabrData resolve(String videoId, PlayerResponse response, WebPlayer.Session web,
                            WebPlayer.Attestation attestation) throws Exception {
        // StreamingData.serverAbrStreamingUrl (15); MediaCommonConfig.mediaUstreamerRequestConfig
        // (2) contains videoPlaybackUstreamerConfig (4).
        byte[] encodedUrl = Proto.bytes(response.streamingData, 15);
        byte[] common = response.mediaCommonConfig;
        byte[] requestConfig = common == null ? null : Proto.bytes(common, 2);
        byte[] ustreamer = requestConfig == null ? null : Proto.bytes(requestConfig, 4);
        if (encodedUrl == null || ustreamer == null || ustreamer.length == 0) {
            throw new IOException("WEB response has no complete SABR configuration");
        }
        Uri uri = Uri.parse(new String(encodedUrl, StandardCharsets.UTF_8));
        if (!StreamUrl.isUsable(uri)) throw new IOException("Invalid SABR streaming URL");
        boolean audio = false;
        boolean video = false;
        // StreamingData.adaptiveFormats (3): Format.mimeType (5), itag (1).
        for (Proto.Field field : Proto.fields(response.streamingData)) {
            if (field.number != 3 || field.wireType != Proto.WIRE_LENGTH) continue;
            byte[] mime = Proto.bytes(field.bytes, 5);
            if (mime == null || Proto.number(field.bytes, 1, 0) <= 0) continue;
            String type = new String(mime, StandardCharsets.UTF_8);
            audio |= type.startsWith("audio/");
            video |= type.startsWith("video/");
        }
        if (!audio || !video) throw new IOException("SABR response has no audio/video format pair");
        String challenge = uri.getQueryParameter("n");
        if (challenge != null) {
            Map<String, String> unlocked = web.solveN(Set.of(challenge));
            uri = Uri.parse(StreamUrl.replaceParameters(uri, Map.of("n", unlocked.get(challenge))));
        }
        byte[] streams = Proto.replace(response.streamingData,
                Map.of(15, uri.toString().getBytes(StandardCharsets.UTF_8)));
        return new SabrData(videoId, streams, SabrPlayback.parseCommonConfig(common), uri, attestation);
    }
}
