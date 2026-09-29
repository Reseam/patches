// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.instagram.links;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class ExternalLinks {
    private static final String LINK_SHIM = "l.instagram.com";
    // Checkout, account login and Bloks forms need the in-app browser's JS bridge.
    private static final String[] IN_APP_DOMAINS = {"instagram.com", "facebook.com", "meta.com"};

    private ExternalLinks() {}

    public static boolean open(Context context, String url) {
        Uri uri = external(url);
        if (uri == null) return false;
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    private static Uri external(String url) {
        Uri uri = web(url);
        if (uri == null) return null;
        String host = uri.getHost().toLowerCase();
        if (host.equals(LINK_SHIM)) return external(uri.getQueryParameter("u"));
        for (String domain : IN_APP_DOMAINS) {
            if (host.equals(domain) || host.endsWith("." + domain)) return null;
        }
        return uri;
    }

    private static Uri web(String url) {
        if (url == null) return null;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        boolean web = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        return web && uri.getHost() != null ? uri : null;
    }
}
