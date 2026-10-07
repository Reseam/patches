// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.spoof;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A direct media URL, with any signature challenge still to be solved. */
record StreamUrl(Uri uri, String signature, String signatureParameter) {
    static StreamUrl parse(PlayerResponse.StreamUrl location) {
        if (location.url() != null) return new StreamUrl(Uri.parse(location.url()), null, null);
        Uri cipher = Uri.parse("https://www.youtube.com/?" + location.signatureCipher());
        String url = cipher.getQueryParameter("url");
        String signature = cipher.getQueryParameter("s");
        String parameter = cipher.getQueryParameter("sp");
        if (url == null || signature == null || signature.isEmpty()) {
            throw new IllegalArgumentException("Incomplete stream signature cipher");
        }
        return new StreamUrl(Uri.parse(url), signature, parameter == null ? "signature" : parameter);
    }

    /** Rejects missing, malformed and expiring media URLs before they reach the native player. */
    static boolean isUsable(Uri uri) {
        String host = uri.getHost();
        return "https".equals(uri.getScheme()) && host != null
                && host.endsWith(".googlevideo.com") && "/videoplayback".equals(uri.getPath())
                && expiresAt(uri) > System.currentTimeMillis() + 60_000;
    }

    static long expiresAt(Uri uri) {
        try {
            return Math.multiplyExact(Long.parseLong(uri.getQueryParameter("expire")), 1000);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return 0;
        }
    }

    /**
     * Replaces only named parameters. Other query bytes, their order, and duplicate parameters
     * stay intact because signatures may cover their original encoding.
     */
    static String replaceParameters(Uri uri, Map<String, String> replacements) {
        List<String> query = new ArrayList<>();
        String original = uri.getEncodedQuery();
        if (original != null && !original.isEmpty()) {
            for (String pair : original.split("&", -1)) {
                int separator = pair.indexOf('=');
                String name = Uri.decode(separator < 0 ? pair : pair.substring(0, separator));
                if (!replacements.containsKey(name)) query.add(pair);
            }
        }
        replacements.forEach((name, value) -> query.add(Uri.encode(name) + "=" + Uri.encode(value)));
        return uri.buildUpon().encodedQuery(String.join("&", query)).build().toString();
    }
}
