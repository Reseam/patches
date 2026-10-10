// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.os.SystemClock;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** The TVHTML5 client identity YouTube serves for {@link #USER_AGENT}, so the client version follows its releases. */
final class TvProfile {
    /**
     * YouTube expects most TV devices to attest: current TVs get "reload the page" and the Xbox One's streams
     * stop at their attestation check. The Wii U profile is served without attestation.
     */
    static final String USER_AGENT = "Mozilla/5.0 (Nintendo WiiU) AppleWebKit/536.30 (KHTML, like Gecko) "
            + "NX/3.0.4.2.12 NintendoBrowser/4.3.1.11264.US";
    private static final String PAGE_URL = "https://www.youtube.com/tv";
    private static final String CONFIG_CALL = "ytcfg.set(";
    private static final long MAX_AGE_MILLISECONDS = TimeUnit.HOURS.toMillis(12);
    private static final int MAX_PAGE_BYTES = 2 * 1024 * 1024;
    /** Device identity only: the page's visitor and session fields belong to its own anonymous page load. */
    private static final List<String> IDENTITY = List.of("clientName", "clientVersion", "deviceMake", "deviceModel",
            "osName", "osVersion", "platform", "userAgent");
    private static JSONObject client;
    private static long loadedAt;

    private TvProfile() {}

    /** A copy of the page's client identity, reloaded once it is older than twelve hours. */
    static synchronized JSONObject get(int timeout) throws Exception {
        if (client == null || SystemClock.elapsedRealtime() - loadedAt >= MAX_AGE_MILLISECONDS) {
            client = load(timeout);
            loadedAt = SystemClock.elapsedRealtime();
        }
        return new JSONObject(client.toString());
    }

    private static JSONObject load(int timeout) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(PAGE_URL).openConnection();
        try {
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("TV page HTTP " + status);
            String page;
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (output.size() + read > MAX_PAGE_BYTES) throw new IOException("TV page is too large");
                    output.write(buffer, 0, read);
                }
                page = output.toString(StandardCharsets.UTF_8.name());
            }
            int start = page.indexOf(CONFIG_CALL);
            if (start < 0) throw new IOException("TV page has no configuration");
            JSONObject config = (JSONObject) new JSONTokener(page.substring(start + CONFIG_CALL.length())).nextValue();
            JSONObject loaded = config.getJSONObject("INNERTUBE_CONTEXT").getJSONObject("client");
            if (!ClientType.TV.clientName.equals(loaded.optString("clientName")) || loaded.optString("clientVersion").isEmpty()) {
                throw new IOException("TV page has no TV client context");
            }
            JSONObject identity = new JSONObject();
            for (String key : IDENTITY) identity.put(key, loaded.opt(key));
            return identity;
        } finally {
            connection.disconnect();
        }
    }
}
