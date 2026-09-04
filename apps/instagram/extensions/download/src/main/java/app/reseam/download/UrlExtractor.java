// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.instagram.download;

import java.util.List;

/**
 * Media URL extraction. Delegates to {@link MediaScanner}, which reads URLs by value (shape)
 * instead of by obfuscated field name, so it does not break across Instagram versions.
 */
final class UrlExtractor {
    private UrlExtractor() {}

    static MediaUrl best(Object media) {
        return MediaScanner.best(media);
    }

    static List<?> carouselChildren(Object media) {
        return MediaScanner.carouselChildren(media);
    }

    static final class MediaUrl {
        final String url;
        final boolean video;

        MediaUrl(String url, boolean video) {
            this.url = url;
            this.video = video;
        }
    }
}
