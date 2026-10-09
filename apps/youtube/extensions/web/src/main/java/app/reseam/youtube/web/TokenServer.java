// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.web;

import android.os.SystemClock;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.core.YouTubeContext;

/**
 * The Reseam token API is provided for apps patched with Reseam, under the terms at
 * https://mint.reseam.app. Other projects may not integrate it, impersonate a Reseam integration,
 * or relay or resell access or issued tokens without prior permission (contact@reseam.app).
 */
final class TokenServer {
    private static final String API = "https://mint.reseam.app/v1/";
    private static final String CREDENTIAL_KEY = "you_tube_settings.token_server_credential";
    private static final int HTTP_TIMEOUT_MILLISECONDS = 10_000;
    private static final int MAX_RESPONSE_BYTES = 64 * 1024;

    record Token(String poToken, long expiresAt) {}

    private record Response(int status, String body) {}

    private TokenServer() {}

    static boolean selected() {
        return "server".equals(Settings.getString("spoof_video_streams_token_source", "device"));
    }

    static Token mint(String videoId, long deadline) throws Exception {
        String body = new JSONObject().put("videoId", videoId).toString();
        Response response = post("tokens", body, credential(deadline), deadline);
        if (response.status == HttpURLConnection.HTTP_UNAUTHORIZED) {
            ReseamSettings.setString(CREDENTIAL_KEY, "");
            response = post("tokens", body, credential(deadline), deadline);
        }
        JSONObject token = success(response);
        long lifetime = Instant.parse(token.getString("expiresAt")).toEpochMilli() - System.currentTimeMillis();
        return new Token(token.getString("poToken"), SystemClock.elapsedRealtime() + lifetime);
    }

    private static synchronized String credential(long deadline) throws Exception {
        String credential = ReseamSettings.getString(CREDENTIAL_KEY, "");
        if (!credential.isEmpty()) return credential;
        String body = new JSONObject().put("packageName", YouTubeContext.get().getPackageName()).toString();
        credential = success(post("installations", body, null, deadline)).getString("credential");
        ReseamSettings.setString(CREDENTIAL_KEY, credential);
        return credential;
    }

    private static JSONObject success(Response response) throws Exception {
        if (response.status / 100 == 2) return new JSONObject(response.body);
        throw new IOException("Token server HTTP " + response.status + ": "
                + response.body.substring(0, Math.min(200, response.body.length())));
    }

    private static Response post(String path, String body, String credential, long deadline) throws Exception {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(API + path).openConnection();
        try {
            int timeout = (int) Math.min(HTTP_TIMEOUT_MILLISECONDS, deadline - SystemClock.elapsedRealtime());
            if (timeout <= 0) throw new TimeoutException("Token server request deadline exceeded");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Reseam/YouTube");
            if (credential != null) connection.setRequestProperty("X-Installation-Token", credential);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }
            int status = connection.getResponseCode();
            try (InputStream input = status < 400 ? connection.getInputStream() : connection.getErrorStream()) {
                return new Response(status, input == null ? "" : read(input));
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > MAX_RESPONSE_BYTES) throw new IOException("Token server response is too large");
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }
}
