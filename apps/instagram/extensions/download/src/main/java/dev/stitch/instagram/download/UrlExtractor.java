/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.instagram.download;

import java.util.List;

import dev.stitch.instagram.refs.Media;

final class UrlExtractor {
    private UrlExtractor() {}

    static MediaUrl best(Object media) {
        String video = Media.videoUrl(media);
        if (video != null && video.startsWith("http")) return new MediaUrl(video, true);

        String photo = Media.photoUrl(media);
        if (photo != null && photo.startsWith("http")) return new MediaUrl(photo, false);

        return null;
    }

    static List<?> carouselChildren(Object media) {
        List<?> children = Media.children(media);
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
