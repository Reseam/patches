// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

import java.util.List;

final class UrlExtractor {
    private UrlExtractor() {}

    static MediaUrl best(Object media) {
        String video = MediaMeta.videoUrl(media);
        if (video != null && video.startsWith("http")) return new MediaUrl(video, true);

        String photo = MediaMeta.imageUrl(media);
        if (photo != null && photo.startsWith("http")) return new MediaUrl(photo, false);

        return null;
    }

    static List<?> carouselChildren(Object media) {
        List<?> children = MediaMeta.carouselChildren(media);
        return children != null && children.size() > 1 ? children : null;
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
