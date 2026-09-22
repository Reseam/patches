// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.core.YouTubeContext;

final class SponsorBlockApi {
    static final ThreadPoolExecutor NETWORK = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16), runnable -> {
                Thread thread = new Thread(runnable, "Reseam-SponsorBlock");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private static volatile long backoffUntil;

    private SponsorBlockApi() {}

    static void execute(Runnable task) {
        try { NETWORK.execute(task); }
        catch (java.util.concurrent.RejectedExecutionException ignored) {
            Logger.debug(() -> "SponsorBlock request queue is full");
        }
    }

    static List<Segment> fetch(String videoId) throws Exception {
        JSONArray categories = new JSONArray();
        for (Segment.Category category : Segment.Category.values()) categories.put(category.key);
        String response = request("GET", "/api/skipSegments", "videoID", videoId,
                "categories", categories.toString(), "actionTypes", "[\"skip\",\"poi\"]");
        JSONArray data = new JSONArray(response);
        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);
            Segment.Category category = Segment.Category.find(item.optString("category"));
            JSONArray times = item.optJSONArray("segment");
            if (category == null || times == null || times.length() != 2) continue;
            double from = times.getDouble(0), to = times.getDouble(1);
            if (!Double.isFinite(from) || !Double.isFinite(to) || from < 0 || to < from) continue;
            segments.add(new Segment(item.getString("UUID"), category,
                    Math.round(from * 1000), Math.round(to * 1000)));
        }
        segments.sort(Comparator.comparingLong(segment -> segment.start));
        return java.util.Collections.unmodifiableList(segments);
    }

    static String request(String method, String path, String... query) throws Exception {
        if (System.currentTimeMillis() < backoffUntil) throw new IOException("SponsorBlock is temporarily rate limited");
        String base = Settings.getString("sb_api_url", "https://sponsor.ajay.app");
        Uri uri = Uri.parse(base);
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null) {
            throw new IOException("SponsorBlock API URL must use HTTPS");
        }
        Uri.Builder builder = uri.buildUpon().appendEncodedPath(path.replaceFirst("^/", ""));
        for (int i = 0; i < query.length; i += 2) builder.appendQueryParameter(query[i], query[i + 1]);
        HttpURLConnection connection = (HttpURLConnection) new URL(builder.build().toString()).openConnection();
        try {
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(7000);
            connection.setRequestMethod(method);
            connection.setRequestProperty("User-Agent", "Reseam/YouTube");
            if ("POST".equals(method)) { connection.setDoOutput(true); connection.setFixedLengthStreamingMode(0); }
            int status = connection.getResponseCode();
            if (status == 404 && "/api/skipSegments".equals(path) && "GET".equals(method)) return "[]";
            if (status == 429) backoffUntil = System.currentTimeMillis() + 10 * 60_000;
            if (status < 200 || status >= 300) throw new IOException("SponsorBlock HTTP " + status);
            try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    if (output.size() + read > 1_048_576) throw new IOException("SponsorBlock response is too large");
                    output.write(buffer, 0, read);
                }
                return output.toString(StandardCharsets.UTF_8.name());
            }
        } finally { connection.disconnect(); }
    }

    static synchronized String userId() {
        String key = "you_tube_settings.sb_private_user_id";
        String id = ReseamSettings.getString(key, "");
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
            ReseamSettings.setString(key, id);
        }
        return id;
    }

    static void toast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(YouTubeContext.get(), message, Toast.LENGTH_LONG).show());
    }
}
