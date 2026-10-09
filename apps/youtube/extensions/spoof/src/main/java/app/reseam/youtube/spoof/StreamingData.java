// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.net.Uri;
import android.os.SystemClock;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** A validated replacement and its provenance, kept together through the native installation hook. */
public final class StreamingData {
    final byte[] response;
    final ClientType client;
    final List<Uri> urls;
    final long expiresAt;
    final SabrData sabr;

    StreamingData(byte[] streamingData, ClientType client) {
        // Direct responses must not advertise a competing SABR transport.
        response = PlayerResponse.withStreamingData(Proto.replace(streamingData,
                Collections.singletonMap(15, null)));
        this.client = client;
        sabr = null;
        urls = PlayerResponse.streamUrls(streamingData).stream().map(url -> Uri.parse(url.url())).collect(Collectors.toList());
        expiresAt = urls.stream().mapToLong(StreamUrl::expiresAt).min().orElse(0);
    }

    StreamingData(SabrData sabr) {
        this.sabr = sabr;
        response = PlayerResponse.withStreamingData(sabr.streams());
        client = sabr.client();
        urls = List.of();
        expiresAt = StreamUrl.expiresAt(sabr.uri());
    }

    /** Leaves time to start playback before the earliest media URL expires. */
    boolean isFresh() {
        return expiresAt > System.currentTimeMillis() + 60_000
                && (sabr == null || sabr.attestation().expiresAt() > SystemClock.elapsedRealtime() + 30_000);
    }
}
