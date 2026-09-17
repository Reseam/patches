// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.misc;

import java.util.regex.Pattern;

import app.reseam.youtube.core.Settings;

/**
 * Avatars and channel images are served from hosts some countries block. They are also on
 * yt4.ggpht.com, which is not blocked, so rewriting the host restores the missing images.
 */
public final class ImageUrl {
    private static final String REPLACEMENT_HOST = "https://yt4.ggpht.com";

    private static final Pattern STATIC_IMAGE_HOST =
            Pattern.compile("^https://(yt3|lh[3-6]|play-lh)\\.(ggpht|googleusercontent)\\.com(?=/|$|[?#])");

    private ImageUrl() {}

    /** Injection point, from the image request constructor. Runs on several threads at once. */
    public static String override(String url) {
        if (url == null || !Settings.getBoolean("bypass_image_region_restrictions", false)) return url;
        return STATIC_IMAGE_HOST.matcher(url).replaceFirst(REPLACEMENT_HOST);
    }
}
