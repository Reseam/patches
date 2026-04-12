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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

final class UrlExtractor {
    private static final String EXTENDED_IMAGE_URL = "com.instagram.model.mediasize.ExtendedImageUrl";
    private static final String VIDEO_URL = "com.instagram.model.mediasize.VideoUrlImpl";

    private UrlExtractor() {}

    static MediaUrl best(Object media) {
        String video = bestVideoUrl(media);
        if (video != null) return new MediaUrl(video, true);

        String photo = bestPhotoUrl(media);
        return photo != null ? new MediaUrl(photo, false) : null;
    }

    @SuppressWarnings("unchecked")
    static List<?> carouselChildren(Object media) {
        for (Field field : Reflect.fields(media.getClass())) {
            if (!List.class.isAssignableFrom(field.getType())) continue;

            Object value = Reflect.fieldValue(field, media);
            if (value instanceof List<?> && ((List<?>) value).size() > 1) return (List<?>) value;
        }
        return null;
    }

    private static String bestVideoUrl(Object media) {
        try {
            Class<?> videoUrlClass = Class.forName(VIDEO_URL);
            VideoCandidate best = null;

            for (Field field : Reflect.fields(media.getClass())) {
                if (!field.getType().isInterface()) continue;

                Object container = Reflect.fieldValue(field, media);
                best = better(best, bestVideoFromContainer(container, videoUrlClass));
            }
            if (best != null) {
                Logcat.d("findVideoUrl: selected score=" + best.score + " from " + best.sourceClass);
                return best.url;
            }
        } catch (Throwable t) {
            Logcat.d("findVideoUrl: " + t);
        }
        return null;
    }

    private static VideoCandidate bestVideoFromContainer(Object container, Class<?> videoUrlClass) {
        if (container == null) return null;

        VideoCandidate best = null;
        for (Method method : Reflect.methods(container.getClass())) {
            if (method.getParameterTypes().length != 0) continue;
            if (!List.class.isAssignableFrom(method.getReturnType())) continue;

            Object versions = Reflect.invoke(method, container);
            if (!(versions instanceof List<?>)) continue;

            for (Object version : (List<?>) versions) {
                best = better(best, videoFromVersion(version, videoUrlClass));
            }
        }
        return best;
    }

    private static VideoCandidate videoFromVersion(Object version, Class<?> videoUrlClass) {
        if (version == null) return null;
        if (videoUrlClass.isInstance(version)) return videoCandidate(version, true);

        for (Field field : Reflect.fields(version.getClass())) {
            if (!videoUrlClass.isAssignableFrom(field.getType())) continue;

            VideoCandidate candidate = videoCandidate(Reflect.fieldValue(field, version), true);
            if (candidate != null) return candidate;
        }
        return videoCandidate(version, false);
    }

    private static VideoCandidate videoCandidate(Object object, boolean knownVideoObject) {
        if (object == null) return null;

        String bestUrl = null;
        for (Field field : Reflect.fields(object.getClass())) {
            if (field.getType() != String.class) continue;

            Object value = Reflect.fieldValue(field, object);
            if (!(value instanceof String)) continue;

            String url = (String) value;
            if (!url.startsWith("http")) continue;
            if (!knownVideoObject && !looksLikeVideoUrl(url)) continue;
            if (bestUrl == null || looksLikeVideoUrl(url)) bestUrl = url;
        }
        return bestUrl == null ? null : new VideoCandidate(bestUrl, qualityScore(object), object.getClass().getName());
    }

    private static String bestPhotoUrl(Object media) {
        try {
            Class<?> imageUrlClass = Class.forName(EXTENDED_IMAGE_URL);
            for (Field field : Reflect.fields(media.getClass())) {
                Object image = Reflect.fieldValue(field, media);
                String url = photoUrl(image, imageUrlClass);
                if (url != null) return url;
            }
        } catch (Throwable t) {
            Logcat.d("findPhotoUrl: " + t);
        }
        return null;
    }

    private static String photoUrl(Object image, Class<?> imageUrlClass) {
        if (image == null) return null;

        if (imageUrlClass.isInstance(image) || looksLikeImageUrl(image)) {
            Object url = Reflect.invoke(noArgMethod(image.getClass(), "getUrl", String.class), image);
            if (url instanceof String && ((String) url).startsWith("http")) return (String) url;
        }
        return httpStringField(image);
    }

    private static Method noArgMethod(Class<?> cls, String name, Class<?> returnType) {
        for (Method method : Reflect.methods(cls)) {
            if (name.equals(method.getName())
                    && method.getParameterTypes().length == 0
                    && returnType.isAssignableFrom(method.getReturnType())) {
                return method;
            }
        }
        return null;
    }

    private static String httpStringField(Object object) {
        for (Field field : Reflect.fields(object.getClass())) {
            if (field.getType() != String.class) continue;

            Object value = Reflect.fieldValue(field, object);
            if (value instanceof String && ((String) value).startsWith("http")) return (String) value;
        }
        return null;
    }

    private static boolean looksLikeVideoUrl(String url) {
        String lower = url.toLowerCase(Locale.US);
        return lower.contains(".mp4")
                || lower.contains("video")
                || lower.contains("fbcdn.net/v/")
                || lower.contains("cdninstagram.com/v/");
    }

    private static boolean looksLikeImageUrl(Object object) {
        String name = object.getClass().getName();
        return name.contains("ImageUrl") || name.contains("Image") || name.contains("mediasize");
    }

    private static long qualityScore(Object object) {
        java.util.ArrayList<Integer> dimensions = new java.util.ArrayList<>();
        long tieBreaker = 0;

        for (Field field : Reflect.fields(object.getClass())) {
            Object value = Reflect.fieldValue(field, object);
            if (!(value instanceof Number)) continue;

            long number = ((Number) value).longValue();
            if (number <= 0) continue;

            tieBreaker += Math.min(number, 10_000_000L);
            if (number >= 64 && number <= 10_000) dimensions.add((int) number);
        }

        int first = 0;
        int second = 0;
        for (Integer dimension : dimensions) {
            int value = dimension;
            if (value > first) {
                second = first;
                first = value;
            } else if (value > second) {
                second = value;
            }
        }

        long area = first > 0 && second > 0 ? (long) first * second : first;
        return area * 10_000_000L + tieBreaker;
    }

    private static VideoCandidate better(VideoCandidate current, VideoCandidate candidate) {
        if (candidate == null) return current;
        if (current == null) return candidate;
        if (candidate.score > current.score) return candidate;
        if (candidate.score == current.score && candidate.url.length() > current.url.length()) return candidate;
        return current;
    }

    static final class MediaUrl {
        final String url;
        final boolean video;

        MediaUrl(String url, boolean video) {
            this.url = url;
            this.video = video;
        }
    }

    private static final class VideoCandidate {
        final String url;
        final long score;
        final String sourceClass;

        VideoCandidate(String url, long score, String sourceClass) {
            this.url = url;
            this.score = score;
            this.sourceClass = sourceClass;
        }
    }
}
