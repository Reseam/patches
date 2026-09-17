// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.misc;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;

import app.reseam.youtube.core.Logger;

/** Removes the parameters YouTube adds to a shared link to attribute the click back to the sharer. */
public final class SharingLinks {
    // "feature" is the older name and may be obsolete, but costs nothing to keep stripping.
    private static final List<String> TRACKING_PARAMETERS = Arrays.asList("si", "feature");

    private SharingLinks() {}

    /** Injection point, from the copy-link path; the patch gates the call on the setting. */
    public static String sanitize(String url) {
        if (url == null) return url;
        try {
            Uri uri = Uri.parse(url);
            String scheme = uri.getScheme();
            // The share sheet also passes the video title through here, which is not a URL.
            if (!"https".equals(scheme) && !"http".equals(scheme)) return url;

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
