// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.misc;

import android.net.Uri;

import app.reseam.youtube.core.Logger;

/** YouTube routes outbound links through youtube.com/redirect, which records the click first. */
public final class UrlRedirects {
    private static final String REDIRECT_PATH = "/redirect";
    private static final String TARGET_PARAMETER = "q";

    private UrlRedirects() {}

    /** Injection point, from the one place a link becomes a Uri; the patch gates it on the setting. */
    public static Uri bypass(Uri uri) {
        if (uri == null || !REDIRECT_PATH.equals(uri.getPath())) return uri;
        String host = uri.getHost();
        if (host == null || !(host.equals("youtube.com") || host.endsWith(".youtube.com"))) return uri;

        String target = uri.getQueryParameter(TARGET_PARAMETER);
        if (target == null) return uri;

        Uri bypassed = Uri.parse(target);
        Logger.debug(() -> "Bypassing redirect " + uri + " to " + bypassed);
        return bypassed;
    }
}
