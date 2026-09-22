// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.refs;

import java.util.List;

public final class Media {
    private Media() {}

    public static String photoUrl(Object media) {
        List<?> candidates = imageCandidates(media);
        if (candidates == null) return null;
        for (Object candidate : candidates) {
            String url = imageCandidateUrl(candidate);
            if (url != null && !url.isEmpty()) return url;
        }
        return null;
    }

    public static List<?> imageCandidates(Object media) { return null; }

    public static String imageCandidateUrl(Object candidate) { return null; }

    public static String videoUrl(Object media) { return null; }

    public static List<?> children(Object media) { return null; }
}
