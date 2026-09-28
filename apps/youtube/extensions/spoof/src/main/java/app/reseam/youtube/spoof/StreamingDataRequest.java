// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.YouTubeContext;
import app.reseam.youtube.web.WebPlayer;

/**
 * Fetches the player response of the first client in the configured order that returns playable
 * streams, while YouTube is building its own player request.
 */
final class StreamingDataRequest {
    private static final String PLAYER_URL = "https://youtubei.googleapis.com/youtubei/v1/player?fields=streamingData&alt=proto";
    private static final String VISITOR_ID_HEADER = "X-Goog-Visitor-Id";
    private static final String[] REQUEST_HEADER_KEYS = {"X-GOOG-API-FORMAT-VERSION", VISITOR_ID_HEADER};
    private static final int HTTP_TIMEOUT_MILLISECONDS = 10_000;
    private static final int MAX_MILLISECONDS_TO_WAIT_FOR_FETCH = 20_000;
    private static final int MAX_CACHED_REQUESTS = 50;
    /** Norwegian Bokmål: a language YouTube does not auto-dub into. */
    private static final Locale ORIGINAL_AUDIO_LOCALE = new Locale("nb");

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Map<String, Future<byte[]>> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, Future<byte[]>>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Future<byte[]>> eldest) {
                    return size() > MAX_CACHED_REQUESTS;
                }
            });

    private static volatile ClientType[] clientOrder = {ClientType.TV_SIMPLY};
    private static volatile boolean preferMultipleAvcQualities;
    private static volatile Locale languageOverride;
    private static volatile ClientType lastSpoofedClient;

    private StreamingDataRequest() {}

    /**
     * With original audio forced, the client is asked for a language YouTube does not dub into, so
     * it answers with the original audio.
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
        languageOverride = forceOriginalAudio ? ORIGINAL_AUDIO_LOCALE : null;
        Logger.debug(() -> "Available spoof clients: " + order);
    }

    static boolean usesWebPlayer() {
        return Arrays.stream(clientOrder).anyMatch(client -> client.usesWebPlayer);
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
        String encodedVisitorData = playerHeaders.get(VISITOR_ID_HEADER);
        String visitorData = encodedVisitorData == null ? null : URLDecoder.decode(encodedVisitorData, StandardCharsets.UTF_8);
        for (ClientType client : clientOrder) {
            if (client.usesWebPlayer && visitorData == null) {
                Logger.debug(() -> "Skipping " + client + " without visitor data to bind its PoToken to");
                continue;
            }
            byte[] response = send(client, videoId, playerHeaders, visitorData);
            byte[] replacement = response == null ? null : playableResponse(client, response, visitorData);
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

    private static byte[] send(ClientType client, String videoId, Map<String, String> playerHeaders,
                               String visitorData) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(PLAYER_URL).openConnection();
            for (String key : REQUEST_HEADER_KEYS) {
                String value = playerHeaders.get(key);
                if (value != null) connection.setRequestProperty(key, value);
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
            byte[] body = innertubeBody(client, videoId, visitorData).getBytes(StandardCharsets.UTF_8);
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
        } catch (Exception exception) {
            Logger.info(() -> "Spoof client " + client + " request failed: " + exception);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String innertubeBody(ClientType client, String videoId, String visitorData) throws Exception {
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
        JSONObject body = new JSONObject()
                .put("context", new JSONObject().put("client", clientJson))
                .put("contentCheckOk", true)
                .put("racyCheckOk", true)
                .put("videoId", videoId);
        if (client.usesWebPlayer) {
            clientJson.put("visitorData", visitorData);
            body.put("playbackContext", new JSONObject().put("contentPlaybackContext",
                    new JSONObject().put("signatureTimestamp", WebPlayer.signatureTimestamp())));
        }
        return body.toString();
    }

    private static byte[] playableResponse(ClientType client, byte[] response, String visitorData) {
        try {
            PlayerResponse parsed = PlayerResponse.parse(response);
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
            if (client.usesWebPlayer) streamingData = unlockWebStreams(streamingData, visitorData);
            return PlayerResponse.withStreamingData(streamingData);
        } catch (IllegalArgumentException exception) {
            Logger.error(() -> "Spoof client " + client + " returned an unreadable response: " + exception);
            return null;
        } catch (Exception exception) {
            Logger.error(() -> "Could not unlock " + client + " streams: " + exception);
            return null;
        }
    }

    /** Solves each URL's `n` challenge and adds a PoToken bound to the visitor data the request carried. */
    private static byte[] unlockWebStreams(byte[] streamingData, String visitorData) throws Exception {
        Set<String> challenges = new HashSet<>();
        for (String url : PlayerResponse.formatUrls(streamingData)) {
            String n = Uri.parse(url).getQueryParameter("n");
            if (n != null) challenges.add(n);
        }
        long start = System.currentTimeMillis();
        WebPlayer.Unlocked unlocked = WebPlayer.unlock(visitorData, challenges);
        Logger.debug(() -> "Unlocked " + challenges.size() + " n challenges in "
                + (System.currentTimeMillis() - start) + " ms");
        return PlayerResponse.rewriteFormatUrls(streamingData, url -> {
            Uri uri = Uri.parse(url);
            Uri.Builder builder = uri.buildUpon().clearQuery();
            for (String name : uri.getQueryParameterNames()) {
                String value = uri.getQueryParameter(name);
                builder.appendQueryParameter(name, name.equals("n") ? Objects.requireNonNull(unlocked.n().get(value)) : value);
            }
            return builder.appendQueryParameter("pot", unlocked.poToken()).build().toString();
        });
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
