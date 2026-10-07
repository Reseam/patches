// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.net.Uri;
import android.os.Looper;
import android.os.SystemClock;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.web.WebPlayer;

/**
 * Fetches direct adaptive streams alongside the native player request. Each request captures
 * its configuration and request identity and shares in-flight work. One deadline covers queueing,
 * web preparation, HTTP and URL resolution across all clients. Completed responses remain usable
 * until their media URLs expire, even when a subsequent request starts a fresh fetch.
 */
final class StreamingDataRequest {
    private static final String PLAYER_URL = "https://youtubei.googleapis.com/youtubei/v1/player?fields=playabilityStatus,streamingData&alt=proto";
    private static final String VISITOR_ID_HEADER = "X-Goog-Visitor-Id";
    private static final int HTTP_TIMEOUT_MILLISECONDS = 10_000;
    private static final int REQUEST_TIMEOUT_MILLISECONDS = 30_000;
    private static final int MAX_CACHED_REQUESTS = 50;
    private static final int MAX_RESPONSE_BYTES = 8 * 1024 * 1024;
    /** Norwegian Bokmål: a language YouTube does not auto-dub into. */
    private static final Locale ORIGINAL_AUDIO_LOCALE = Locale.forLanguageTag("nb");

    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_CACHED_REQUESTS));
    private static final ScheduledThreadPoolExecutor DEADLINES = new ScheduledThreadPoolExecutor(1);
    /** Accessed under StreamingDataRequest.class, including configuration changes. */
    private static final Map<String, Request> REQUESTS = new LinkedHashMap<>();
    private static Configuration configuration = new Configuration(Arrays.asList(ClientType.values()), false, null);
    private static Map<String, String> requestHeaders = Collections.emptyMap();
    private static long generation;

    static {
        DEADLINES.setRemoveOnCancelPolicy(true);
    }

    private record Configuration(List<ClientType> clients, boolean preferAvc, Locale locale) {}

    private StreamingDataRequest() {}

    /** Changes apply atomically; requests from an earlier configuration cannot be reused. */
    static synchronized void setClientOrder(ClientType preferred, List<ClientType> available,
                                            boolean preferMultipleAvc, boolean forceOriginalAudio) {
        List<ClientType> order = new ArrayList<>();
        order.add(preferred);
        for (ClientType client : available) {
            if (!order.contains(client)) order.add(client);
        }
        Configuration updated = new Configuration(Collections.unmodifiableList(order), preferMultipleAvc,
                forceOriginalAudio ? ORIGINAL_AUDIO_LOCALE : null);
        if (updated.equals(configuration)) return;
        clearRequests();
        configuration = updated;
        Logger.debug(() -> "Available spoof clients: " + order);
    }

    static synchronized boolean usesWebPlayer() {
        return configuration.clients.stream().anyMatch(client -> client.usesWebPlayer);
    }

    static synchronized String getClientOsName() {
        return configuration.clients.get(0).osName;
    }

    static synchronized void fetchRequest(String videoId, Map<String, String> playerHeaders) {
        // HTTP field names are case-insensitive. Do not forward native authorization or cookies.
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (playerHeaders != null) playerHeaders.forEach((key, value) -> {
            if (key != null && value != null && (key.equalsIgnoreCase(VISITOR_ID_HEADER)
                    || key.equalsIgnoreCase("X-GOOG-API-FORMAT-VERSION"))) headers.put(key, value);
        });
        String visitor = headers.get(VISITOR_ID_HEADER);
        // Percent escapes occur in native requests; '+' is part of base64 and must stay intact.
        if (visitor != null) headers.put(VISITOR_ID_HEADER, Uri.decode(visitor));
        if (!requestHeaders.equals(headers)) {
            clearRequests();
            requestHeaders = headers;
        }
        Iterator<Request> iterator = REQUESTS.values().iterator();
        while (iterator.hasNext()) {
            Request request = iterator.next();
            if (request.isExpired()) {
                request.cancel();
                iterator.remove();
            }
        }
        Request previous = REQUESTS.get(videoId);
        if (previous != null && !previous.task.isDone() && previous.headers.equals(headers)) return;
        if (previous != null) {
            REQUESTS.remove(videoId);
            previous.cancel();
        }
        if (REQUESTS.size() >= MAX_CACHED_REQUESTS) {
            Request oldest = REQUESTS.remove(REQUESTS.keySet().iterator().next());
            oldest.cancel();
        }
        Request request = new Request(videoId, headers, configuration, generation);
        REQUESTS.put(videoId, request);
        request.timeout = DEADLINES.schedule(request::cancel, REQUEST_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        try {
            EXECUTOR.execute(request.task);
        } catch (RuntimeException exception) {
            request.cancel();
            REQUESTS.remove(videoId, request);
            Logger.error(() -> "Could not schedule stream request", exception);
        }
    }

    /** Returns the matching replacement, or null on failure; never blocks the UI thread. */
    static StreamingData get(String videoId) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Logger.error(() -> "Cannot await spoof streams on the main thread");
            return null;
        }
        Request request;
        synchronized (StreamingDataRequest.class) {
            request = REQUESTS.get(videoId);
        }
        if (request == null) return null;
        try {
            // FutureTask returns an already completed result even with a zero wait budget.
            // The fetch deadline bounds unfinished work, not the lifetime of its media URLs.
            long wait = Math.max(0, request.deadline - SystemClock.elapsedRealtime());
            StreamingData result = request.task.get(wait, TimeUnit.MILLISECONDS);
            synchronized (StreamingDataRequest.class) {
                if (request.generation != generation) return null;
                if (result != null && result.isFresh()) return result;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception exception) {
            Logger.error(() -> "Spoof stream response failed for " + videoId + ": " + exception);
        }
        synchronized (StreamingDataRequest.class) {
            REQUESTS.remove(videoId, request);
        }
        request.cancel();
        return null;
    }

    private static void clearRequests() {
        // Also invalidates completed results held by waiters after their cache entry was replaced.
        generation++;
        REQUESTS.values().forEach(Request::cancel);
        REQUESTS.clear();
    }

    private static int remaining(long deadline) throws InterruptedException, TimeoutException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Stream request cancelled");
        long remaining = deadline - SystemClock.elapsedRealtime();
        if (remaining <= 0) throw new TimeoutException("Stream request deadline exceeded");
        return (int) Math.min(remaining, Integer.MAX_VALUE);
    }

    private static final class Request {
        final String videoId;
        final Map<String, String> headers;
        final Configuration config;
        final long generation;
        final Locale locale;
        final long deadline = SystemClock.elapsedRealtime() + REQUEST_TIMEOUT_MILLISECONDS;
        final FutureTask<StreamingData> task;
        /** Published before task completion; failed or cancelled tasks never expose it to callers. */
        volatile StreamingData completed;
        volatile HttpURLConnection connection;
        volatile ScheduledFuture<?> timeout;

        Request(String videoId, Map<String, String> headers, Configuration config, long generation) {
            this.videoId = videoId;
            this.headers = headers;
            this.config = config;
            this.generation = generation;
            locale = config.locale == null ? Locale.getDefault() : config.locale;
            task = new FutureTask<>(() -> {
                completed = fetch();
                return completed;
            }) {
                @Override
                protected void done() {
                    ScheduledFuture<?> scheduled = timeout;
                    if (scheduled != null) scheduled.cancel(false);
                }
            };
        }

        /** Pending work uses its fetch deadline; successful work uses its media expiry. */
        boolean isExpired() {
            long now = SystemClock.elapsedRealtime();
            if (!task.isDone()) return deadline <= now;
            StreamingData data = completed;
            return task.isCancelled() || data == null || !data.isFresh();
        }

        void cancel() {
            task.cancel(true);
            EXECUTOR.remove(task);
            HttpURLConnection active = connection;
            if (active != null) active.disconnect();
        }

        StreamingData fetch() throws Exception {
            String visitor = headers.get(VISITOR_ID_HEADER);
            for (int index = 0; index < config.clients.size(); index++) {
                ClientType client = config.clients.get(index);
                if (client.usesWebPlayer && (visitor == null || visitor.isEmpty())) continue;
                // Reserve a share of the remaining budget for every fallback client.
                long attemptDeadline = SystemClock.elapsedRealtime() + remaining(deadline) / (config.clients.size() - index);
                try (WebPlayer.Session web = client.usesWebPlayer ? WebPlayer.open(attemptDeadline) : null) {
                    byte[] response = send(client, visitor, web, attemptDeadline);
                    remaining(attemptDeadline);
                    PlayerResponse parsed = PlayerResponse.parse(response);
                    if (parsed.status != PlayerResponse.STATUS_OK || parsed.streamingData == null) {
                        Logger.info(() -> "Spoof client " + client + " has no playable response, status "
                                + parsed.status + (parsed.reason == null ? "" : ": " + parsed.reason));
                        continue;
                    }
                    byte[] streams = resolveStreams(parsed.streamingData, visitor, web);
                    if (config.preferAvc) streams = PlayerResponse.preferMultipleAvcQualities(streams);
                    remaining(attemptDeadline);
                    if (!PlayerResponse.hasAdaptiveStreams(streams)) {
                        Logger.info(() -> "Spoof client " + client + " has no resolved adaptive audio/video pair");
                        continue;
                    }
                    Logger.debug(() -> "Built stream response for " + videoId + " using " + client.friendlyName);
                    return new StreamingData(streams, client);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw exception;
                } catch (Exception exception) {
                    Logger.info(() -> "Spoof client " + client + " failed for " + videoId + ": " + exception);
                }
            }
            Logger.error(() -> "No compatible client streams for " + videoId);
            return null;
        }

        private byte[] send(ClientType client, String visitor, WebPlayer.Session web, long attemptDeadline) throws Exception {
            byte[] body = innertubeBody(client, videoId, visitor, locale, web).getBytes(StandardCharsets.UTF_8);
            HttpURLConnection active = (HttpURLConnection) new URL(PLAYER_URL).openConnection();
            connection = active;
            ScheduledFuture<?> attemptTimeout = null;
            try {
                int budget = remaining(attemptDeadline);
                attemptTimeout = DEADLINES.schedule(active::disconnect, budget, TimeUnit.MILLISECONDS);
                active.setConnectTimeout(Math.min(HTTP_TIMEOUT_MILLISECONDS, budget));
                active.setReadTimeout(Math.min(HTTP_TIMEOUT_MILLISECONDS, budget));
                active.setUseCaches(false);
                active.setDoOutput(true);
                active.setRequestMethod("POST");
                headers.forEach(active::setRequestProperty);
                active.setRequestProperty("Content-Type", "application/json");
                active.setRequestProperty("User-Agent", client.userAgent);
                active.setRequestProperty("X-YouTube-Client-Name", Integer.toString(client.clientId));
                active.setRequestProperty("X-YouTube-Client-Version", client.clientVersion);
                active.setFixedLengthStreamingMode(body.length);
                try (OutputStream output = active.getOutputStream()) {
                    output.write(body);
                }
                remaining(attemptDeadline);
                int status = active.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) throw new IOException("Player HTTP " + status);
                try (InputStream input = active.getInputStream()) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        remaining(attemptDeadline);
                        if (output.size() + read > MAX_RESPONSE_BYTES) throw new IOException("Player response is too large");
                        output.write(buffer, 0, read);
                    }
                    return output.toByteArray();
                }
            } finally {
                if (attemptTimeout != null) attemptTimeout.cancel(false);
                active.disconnect();
                connection = null;
            }
        }
    }

    private static String innertubeBody(ClientType client, String videoId, String visitor, Locale locale,
                                       WebPlayer.Session web) throws Exception {
        JSONObject clientJson = new JSONObject()
                .put("deviceMake", client.deviceMake)
                .put("deviceModel", client.deviceModel)
                .put("clientName", client.clientName)
                .put("clientVersion", client.clientVersion)
                .put("osName", client.osName)
                .put("osVersion", client.osVersion)
                .put("userAgent", client.userAgent)
                .put("visitorData", visitor)
                .put("hl", locale.getLanguage())
                .put("gl", locale.getCountry());
        JSONObject body = new JSONObject()
                .put("context", new JSONObject().put("client", clientJson))
                .put("contentCheckOk", true)
                .put("racyCheckOk", true)
                .put("videoId", videoId);
        if (web != null) {
            body.put("playbackContext", new JSONObject().put("contentPlaybackContext",
                    new JSONObject().put("signatureTimestamp", web.signatureTimestamp())));
        }
        return body.toString();
    }

    private static byte[] resolveStreams(byte[] streamingData, String visitor, WebPlayer.Session web) throws Exception {
        Map<PlayerResponse.StreamUrl, StreamUrl> streams = new LinkedHashMap<>();
        for (PlayerResponse.StreamUrl location : PlayerResponse.streamUrls(streamingData)) {
            try {
                StreamUrl stream = StreamUrl.parse(location);
                if (StreamUrl.isUsable(stream.uri()) && (web != null || stream.signature() == null)) streams.put(location, stream);
            } catch (IllegalArgumentException exception) {
                Logger.debug(() -> "Ignoring malformed stream URL");
            }
        }
        if (streams.isEmpty()) throw new IOException("Player response has no usable stream URLs");
        if (web == null) {
            return PlayerResponse.withResolvedUrls(streamingData, location -> {
                StreamUrl stream = streams.get(location);
                return stream == null ? null : stream.uri().toString();
            });
        }
        Set<String> n = new HashSet<>();
        Set<String> sig = new HashSet<>();
        for (StreamUrl stream : streams.values()) {
            String challenge = stream.uri().getQueryParameter("n");
            if (challenge != null) n.add(challenge);
            if (stream.signature() != null) sig.add(stream.signature());
        }
        WebPlayer.Unlocked unlocked = web.unlock(visitor, n, sig);
        return PlayerResponse.withResolvedUrls(streamingData, location -> {
            StreamUrl stream = streams.get(location);
            if (stream == null) return null;
            Map<String, String> replacements = new LinkedHashMap<>();
            String challenge = stream.uri().getQueryParameter("n");
            if (challenge != null) replacements.put("n", solution(unlocked.n(), challenge));
            if (stream.signature() != null) {
                replacements.put(stream.signatureParameter(), solution(unlocked.sig(), stream.signature()));
            }
            replacements.put("pot", unlocked.poToken());
            return StreamUrl.replaceParameters(stream.uri(), replacements);
        });
    }

    private static String solution(Map<String, String> solutions, String challenge) {
        String solution = solutions.get(challenge);
        if (solution == null || solution.isEmpty()) throw new IllegalArgumentException("Missing player challenge solution");
        return solution;
    }
}
