// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.links;

import android.content.Intent;
import android.net.Uri;

import java.util.Arrays;
import java.util.List;

import app.reseam.youtube.core.Logger;

/** Removes the parameters YouTube adds to a shared link to attribute the click back to the sharer. */
public final class SharingLinks {
    // "feature" is the older name and may be obsolete, but costs nothing to keep stripping.
    private static final List<String> TRACKING_PARAMETERS = Arrays.asList("si", "feature");

    private SharingLinks() {}

    /** Injection point, from the share intents; the patch gates the call on the setting. */
    public static String sanitizeExtra(String key, String value) {
        return Intent.EXTRA_TEXT.equals(key) ? sanitize(value) : value;
    }

    private static boolean isYouTube(String host) {
        return host != null && (host.equals("youtu.be") || host.equals("youtube.com") || host.endsWith(".youtube.com"));
    }

    /** Injection point, from the copy-link path; the patch gates the call on the setting. */
    public static String sanitize(String url) {
        if (url == null) return url;
        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            // The share sheet also passes the video title through here, which is not a URL.
            if (!"https".equals(scheme) && !"http".equals(scheme)) return url;
            // Other sites may use these parameter names for real content.
            if (!isYouTube(uri.getHost())) return url;

            Uri.Builder builder = uri.buildUpon().clearQuery();
            for (String name : uri.getQueryParameterNames()) {
                if (TRACKING_PARAMETERS.contains(name)) continue;
                for (String value : uri.getQueryParameters(name)) {
                    builder.appendQueryParameter(name, value);
                }
            }
            String sanitized = builder.build().toString();
            Logger.debug(() -> "Sanitized " + url + " to " + sanitized);
            return sanitized;
        } catch (Exception ex) {
            Logger.error(() -> "Could not sanitize " + url + ": " + ex);
            return url;
        }
    }
}
