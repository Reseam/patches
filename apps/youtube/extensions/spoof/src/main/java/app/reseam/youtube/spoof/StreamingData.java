// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.net.Uri;

import java.util.List;
import java.util.stream.Collectors;

/** A validated replacement and its provenance, kept together through the native installation hook. */
public final class StreamingData {
    final byte[] response;
    final ClientType client;
    final List<Uri> urls;
    final long expiresAt;

    StreamingData(byte[] streamingData, ClientType client) {
        response = PlayerResponse.withStreamingData(streamingData);
        this.client = client;
        urls = PlayerResponse.streamUrls(streamingData).stream().map(url -> Uri.parse(url.url())).collect(Collectors.toList());
        expiresAt = urls.stream().mapToLong(StreamUrl::expiresAt).min().orElse(0);
    }

    /** Leaves time to start playback before the earliest media URL expires. */
    boolean isFresh() {
        return expiresAt > System.currentTimeMillis() + 60_000;
    }
}
