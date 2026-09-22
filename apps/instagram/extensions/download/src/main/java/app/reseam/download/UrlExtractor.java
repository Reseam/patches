// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.instagram.download;

import app.reseam.instagram.refs.Media;

import java.util.List;

final class UrlExtractor {
    private UrlExtractor() {}

    static MediaUrl best(Object media) {
        if (media == null) return null;
        String video = Media.videoUrl(media);
        if (video != null && !video.isEmpty()) return new MediaUrl(video, true);
        String photo = Media.photoUrl(media);
        return photo == null || photo.isEmpty() ? null : new MediaUrl(photo, false);
    }

    static List<?> carouselChildren(Object media) {
        return media == null ? null : Media.children(media);
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
