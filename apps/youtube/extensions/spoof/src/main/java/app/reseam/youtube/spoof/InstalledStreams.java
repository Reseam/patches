// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.net.Uri;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Tracks installed responses separately from speculative fetches and their short-lived cache. */
final class InstalledStreams {
    private static final int MAX_RESPONSES = 50;
    private static final Map<Set<Key>, Entry> RESPONSES = new LinkedHashMap<>();
    private static final Map<String, Entry> CLIENTS = new LinkedHashMap<>();
    private static String currentVideoId;

    private record Entry(String videoId, ClientType client, long expiresAt, Set<Key> streams) {}

    /** Stable signed identity; native requests may add range, request-number and buffer parameters. */
    private record Key(String id, String itag, String expires, String signature) {
        static Key of(Uri uri) {
            if (!"/videoplayback".equals(uri.getPath())) return null;
            String host = uri.getHost();
            if (host == null || !host.endsWith(".googlevideo.com")) return null;
            String id = uri.getQueryParameter("id");
            String itag = uri.getQueryParameter("itag");
            String expires = uri.getQueryParameter("expire");
            String signature = uri.getQueryParameter("sig");
            if (signature == null) signature = uri.getQueryParameter("signature");
            if (id == null || itag == null || expires == null || signature == null) return null;
            return new Key(id, itag, expires, signature);
        }
    }

    private InstalledStreams() {}

    static synchronized void installed(String videoId, StreamingData data) {
        prune();
        Entry entry = new Entry(videoId, data.client, data.expiresAt,
                data.urls.stream().map(Key::of).filter(Objects::nonNull).collect(Collectors.toSet()));
        // Refreshes may overlap an older response's in-flight media requests.
        RESPONSES.remove(entry.streams);
        RESPONSES.put(entry.streams, entry);
        CLIENTS.remove(videoId);
        CLIENTS.put(videoId, entry);
        if (RESPONSES.size() > MAX_RESPONSES) {
            Entry oldest = RESPONSES.values().stream().filter(value -> !value.videoId.equals(currentVideoId))
                    .findFirst().orElse(RESPONSES.values().iterator().next());
            RESPONSES.remove(oldest.streams);
            CLIENTS.remove(oldest.videoId, oldest);
        }
        if (CLIENTS.size() > MAX_RESPONSES) {
            CLIENTS.keySet().stream().filter(id -> !id.equals(currentVideoId)).findFirst().ifPresent(CLIENTS::remove);
        }
    }

    static synchronized void nativeInstalled(String videoId) {
        CLIENTS.remove(videoId);
    }

    static synchronized void videoChanged(String videoId) {
        currentVideoId = videoId;
    }

    static synchronized String clientName() {
        prune();
        Entry entry = CLIENTS.get(currentVideoId);
        return entry == null ? "Native" : entry.client.friendlyName;
    }

    static synchronized boolean owns(Uri uri) {
        prune();
        Key key = Key.of(uri);
        return key != null && RESPONSES.values().stream().anyMatch(entry -> entry.streams.contains(key));
    }

    private static void prune() {
        long now = System.currentTimeMillis();
        RESPONSES.values().removeIf(entry -> entry.expiresAt <= now);
        CLIENTS.values().removeIf(entry -> entry.expiresAt <= now);
    }
}
