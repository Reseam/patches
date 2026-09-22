// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.dislike.requests;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.json.JSONObject;

final class Requester {
    private Requester() {}
    static HttpURLConnection getConnectionFromRoute(String base, Route route, String... parameters) throws IOException {
        Route.CompiledRoute compiled = route.compile(parameters);
        HttpURLConnection connection = (HttpURLConnection) new URL(base + compiled.getCompiledRoute()).openConnection();
        connection.setRequestMethod(compiled.getMethod().name());
        connection.setRequestProperty("User-Agent", "Reseam/YouTube");
        return connection;
    }
    static JSONObject parseJSONObject(HttpURLConnection connection) throws IOException, JSONException {
        return new JSONObject(parseStringAndDisconnect(connection));
    }
    static String parseStringAndDisconnect(HttpURLConnection connection) throws IOException {
        try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                if (output.size() + read > 1_048_576) throw new IOException("RYD response is too large");
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } finally { connection.disconnect(); }
    }
}
