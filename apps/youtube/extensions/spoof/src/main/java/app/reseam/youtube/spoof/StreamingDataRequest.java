// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.YouTubeContext;

/**
 * Fetches the player response of the first client in the configured order that returns playable
 * streams, while YouTube is building its own player request.
 */
final class StreamingDataRequest {
    private static final String API_URL = "https://youtubei.googleapis.com/youtubei/v1/";
    private static final String PLAYER_ROUTE = "player?fields=streamingData&alt=proto";
    private static final String REEL_ROUTE = "reel/reel_item_watch"
            + "?fields=playerResponse.playabilityStatus,playerResponse.streamingData&alt=proto";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String[] REQUEST_HEADER_KEYS = {
            AUTHORIZATION_HEADER, "X-GOOG-API-FORMAT-VERSION", "X-Goog-Visitor-Id",
    };
    private static final int HTTP_TIMEOUT_MILLISECONDS = 10_000;
    private static final int MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 20_000;
    private static final int MAX_CACHED_REQUESTS = 50;
    /** Norwegian Bokmål: not auto-dubbed by YouTube, and a language Android VR supports. */
    private static final Locale ORIGINAL_AUDIO_LOCALE = new Locale("nb");

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Map<String, Future<byte[]>> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, Future<byte[]>>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Future<byte[]>> eldest) {
                    return size() > MAX_CACHED_REQUESTS;
                }
            });

    private static volatile ClientType[] clientOrder = {ClientType.ANDROID_REEL_NO_AUTH};
    private static volatile boolean preferMultipleAvcQualities;
    private static volatile Locale languageOverride;
    private static volatile ClientType lastSpoofedClient;

    private StreamingDataRequest() {}

    /**
     * With original audio forced, an unauthenticated client without multiple audio tracks is asked
     * for a language YouTube does not dub into, so it answers with the original audio.
     */
    static void setClientOrder(ClientType preferred, List<ClientType> available,
                               boolean preferMultipleAvc, boolean forceOriginalAudio) {
        List<ClientType> order = new ArrayList<>();
        order.add(preferred);
        for (ClientType client : available) {
            if (client != preferred) order.add(client);
        }
        clientOrder = order.toArray(new ClientType[0]);
        preferMultipleAvcQualities = preferMultipleAvc;
        languageOverride = forceOriginalAudio && !preferred.useAuth && !preferred.supportsMultiAudioTracks
                ? ORIGINAL_AUDIO_LOCALE
                : null;
        Logger.debug(() -> "Available spoof clients: " + order);
    }

    static String getClientOsName() {
        return clientOrder[0].osName;
    }

    static String getLastSpoofedClientName() {
        ClientType client = lastSpoofedClient;
        return client == null ? "Unknown" : client.friendlyName;
    }

    static void fetchRequest(String videoId, Map<String, String> playerHeaders) {
        Map<String, String> headers = playerHeaders == null ? Collections.emptyMap() : new HashMap<>(playerHeaders);
        CACHE.put(videoId, EXECUTOR.submit(() -> fetch(videoId, headers)));
    }

    /** The replacement player response for `videoId`, or null when none was fetched. */
    static byte[] get(String videoId) {
        Future<byte[]> request = CACHE.get(videoId);
        if (request == null) return null;
        try {
            return request.get(MAX_MILLISECONDS_TO_WAIT_FOR_FETCH, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            Logger.error(() -> "Spoof stream response failure: " + exception);
        }
        return null;
    }

    private static byte[] fetch(String videoId, Map<String, String> playerHeaders) {
        for (ClientType client : clientOrder) {
            byte[] response = send(client, videoId, playerHeaders);
            byte[] replacement = response == null ? null : playableResponse(client, response);
            if (replacement != null) {
                lastSpoofedClient = client;
                Logger.debug(() -> "Spoofed player response built for " + videoId + " using " + client.friendlyName);
                return replacement;
            }
        }
        lastSpoofedClient = null;
        Logger.error(() -> "Could not fetch any client streams for " + videoId);
        showToast("Could not fetch any client streams");
        return null;
    }

    private static byte[] send(ClientType client, String videoId, Map<String, String> playerHeaders) {
        HttpURLConnection connection = null;
        try {
            boolean authorized = false;
            connection = (HttpURLConnection) new URL(API_URL + (client.usePlayerEndpoint ? PLAYER_ROUTE : REEL_ROUTE))
                    .openConnection();
            for (String key : REQUEST_HEADER_KEYS) {
                String value = playerHeaders == null ? null : playerHeaders.get(key);
                if (value == null) continue;
                if (key.equals(AUTHORIZATION_HEADER)) {
                    if (!client.useAuth) continue;
                    authorized = true;
                }
                connection.setRequestProperty(key, value);
            }
            if (client.useAuth && !authorized) {
                Logger.debug(() -> "Skipping " + client + " since the user is not signed in");
                return null;
            }
            connection.setConnectTimeout(HTTP_TIMEOUT_MILLISECONDS);
            connection.setReadTimeout(HTTP_TIMEOUT_MILLISECONDS);
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", client.userAgent);
            // The client name, not its numeric id, is what this header carries.
            connection.setRequestProperty("X-YouTube-Client-Name", client.clientName);
            connection.setRequestProperty("X-YouTube-Client-Version", client.clientVersion);

            Logger.debug(() -> "Fetching video streams for: " + videoId + " using client: " + client.friendlyName);
            byte[] body = innertubeBody(client, videoId).getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String message = connection.getResponseMessage();
                Logger.info(() -> "Spoof client " + client + " returned HTTP " + responseCode + " " + message);
                return null;
            }
            if (connection.getContentLength() == 0) return null;
            return readAll(connection.getInputStream());
        } catch (IOException | JSONException exception) {
            Logger.info(() -> "Spoof client " + client + " request failed: " + exception);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String innertubeBody(ClientType client, String videoId) throws JSONException {
        Locale locale = languageOverride == null ? Locale.getDefault() : languageOverride;
        JSONObject clientJson = new JSONObject()
                .put("deviceMake", client.deviceMake)
                .put("deviceModel", client.deviceModel)
                .put("clientName", client.clientName)
                .put("clientVersion", client.clientVersion)
                .put("osName", client.osName)
                .put("osVersion", client.osVersion)
                .put("hl", locale.getLanguage())
                .put("gl", locale.getCountry());
        if (client.androidSdkVersion != null) clientJson.put("androidSdkVersion", client.androidSdkVersion);
        JSONObject body = new JSONObject().put("context", new JSONObject().put("client", clientJson));
        JSONObject playerRequest = client.usePlayerEndpoint ? body : new JSONObject();
        playerRequest.put("contentCheckOk", true).put("racyCheckOk", true).put("videoId", videoId);
        if (!client.usePlayerEndpoint) body.put("playerRequest", playerRequest).put("disablePlayerResponse", false);
        return body.toString();
    }

    private static byte[] playableResponse(ClientType client, byte[] response) {
        try {
            byte[] playerResponse = client.usePlayerEndpoint ? response : PlayerResponse.unwrapReel(response);
            if (playerResponse == null) return null;
            PlayerResponse parsed = PlayerResponse.parse(playerResponse);
            if (parsed.streamingData == null) {
                Logger.debug(() -> "Ignoring " + client + " without streaming data, status "
                        + parsed.status + (parsed.reason == null ? "" : ": " + parsed.reason));
                return null;
            }
            if (PlayerResponse.adaptiveFormatCount(parsed.streamingData) == 0) {
                Logger.debug(() -> "Ignoring " + client + " without adaptive formats");
                return null;
            }
            byte[] streamingData = preferMultipleAvcQualities
                    ? PlayerResponse.preferMultipleAvcQualities(parsed.streamingData)
                    : parsed.streamingData;
            return PlayerResponse.withStreamingData(streamingData);
        } catch (IllegalArgumentException exception) {
            Logger.error(() -> "Spoof client " + client + " returned an unreadable response: " + exception);
            return null;
        }
    }

    private static void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(YouTubeContext.get(), message, Toast.LENGTH_SHORT).show());
    }

    private static byte[] readAll(InputStream input) throws IOException {
        try (InputStream stream = input) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toByteArray();
        }
    }
}
