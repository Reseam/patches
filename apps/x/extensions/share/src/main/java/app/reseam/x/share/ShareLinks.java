// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.share;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import app.reseam.runtime.settings.ReseamSettings;

/**
 * Moves post links in outgoing shares to the embed-friendly host chosen in settings. Only links to
 * a post (a path with /status/) change; lists, trends, profiles and every other link pass through.
 */
public final class ShareLinks {
    private static final String OFF = "off";
    private static volatile String providerKey;

    private ShareLinks() {}

    public static void configure(String key) {
        providerKey = key;
    }

    public static String rewrite(String link) {
        String key = providerKey;
        if (key == null || link == null) return link;
        String provider = ReseamSettings.getChoice(key);
        if (OFF.equals(provider)) return link;
        URI uri;
        try {
            uri = new URI(link.trim());
        } catch (URISyntaxException e) {
            return link;
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        String path = uri.getRawPath();
        if (scheme == null || host == null || path == null) return link;
        if (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http")) return link;
        if (!isXHost(host.toLowerCase(Locale.ROOT)) || !path.contains("/status/")) return link;
        StringBuilder out = new StringBuilder("https://").append(provider).append(path);
        if (uri.getRawQuery() != null) out.append('?').append(uri.getRawQuery());
        if (uri.getRawFragment() != null) out.append('#').append(uri.getRawFragment());
        return out.toString();
    }

    private static boolean isXHost(String host) {
        return host.equals("x.com") || host.equals("www.x.com") || host.equals("twitter.com")
                || host.equals("www.twitter.com") || host.equals("mobile.twitter.com");
    }
}
