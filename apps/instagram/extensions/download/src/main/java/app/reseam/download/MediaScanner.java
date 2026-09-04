// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.instagram.download;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Extracts media download URLs from a parsed Instagram media object by VALUE, not by obfuscated
 * field name, so it does not break across app versions. Works for feed, reels, and stories
 * because they share the same URL schema, and for private accounts because it only ever reads
 * the media the app itself parsed.
 *
 * Two schemas matter and must not be confused:
 *   - carousel_media: a list of child MEDIA objects (the album slides). Same class as the parent.
 *   - image_versions2.candidates / video_versions: a list of the SAME media at different
 *     resolutions. We download only the highest.
 */
final class MediaScanner {
    private MediaScanner() {}

    private static final int MAX_DEPTH = 8;

    /** Highest-resolution download URL for one media item. Video wins over image when both exist. */
    static UrlExtractor.MediaUrl best(Object media) {
        if (media == null) return null;
        List<Cand> cands = new ArrayList<>();
        // Don't descend into the carousel-child list: those are sibling slides, handled separately.
        collect(media, media.getClass(), 0, new IdentityHashMap<Object, Boolean>(), cands);
        Cand video = null, image = null;
        for (Cand c : cands) {
            if (c.video) { if (video == null || c.score > video.score) video = c; }
            else { if (image == null || c.score > image.score) image = c; }
        }
        Cand pick = video != null ? video : image;
        return pick == null ? null : new UrlExtractor.MediaUrl(pick.url, pick.video);
    }

    /** The album's child media (same class as the parent), or null if this is not a carousel. */
    static List<?> carouselChildren(Object media) {
        if (media == null) return null;
        List<List<?>> lists = new ArrayList<>();
        findSameClassLists(media, media.getClass(), 0, new IdentityHashMap<Object, Boolean>(), lists);
        List<?> largest = null;
        for (List<?> l : lists) if (largest == null || l.size() > largest.size()) largest = l;
        return (largest != null && largest.size() >= 2) ? largest : null;
    }

    private static final class Cand {
        final String url; final boolean video; final long score;
        Cand(String u, boolean v, long s) { url = u; video = v; score = s; }
    }

    // Walk the media graph collecting URL candidates. `rootClass` is the media class; we never
    // descend into a list of that class (the carousel siblings), keeping each item's URLs isolated.
    private static void collect(Object o, Class<?> rootClass, int depth, IdentityHashMap<Object, Boolean> seen, List<Cand> out) {
        if (o == null || depth > MAX_DEPTH || out.size() > 400) return;
        if (o instanceof String || o instanceof Number || o instanceof Boolean) return;
        if (seen.put(o, Boolean.TRUE) != null) return;

        if (o instanceof List) {
            List<?> l = (List<?>) o;
            if (!l.isEmpty() && rootClass.isInstance(l.get(0))) return; // carousel siblings, skip
            for (int i = 0; i < l.size() && i < 30; i++) collect(l.get(i), rootClass, depth + 1, seen, out);
            return;
        }
        String cn = o.getClass().getName();
        if (cn.startsWith("java.") || cn.startsWith("android.") || cn.startsWith("kotlin.")) return;

        Field[] fields = declaredFields(o.getClass());
        long a = 0, b = 0, bandwidth = 0;   // two largest small ints = width/height; large int = bitrate
        List<String> urls = new ArrayList<>();
        for (Field f : fields) {
            Object v = read(f, o);
            if (v instanceof String) { if (isMediaUrl((String) v)) urls.add((String) v); }
            else if (v instanceof Integer || v instanceof Long) {
                long n = ((Number) v).longValue();
                if (n > 0 && n < 20000) { if (n > a) { b = a; a = n; } else if (n > b) b = n; }
                else if (n >= 20000) bandwidth = Math.max(bandwidth, n);
            }
        }
        // A real image candidate or video version carries its own width and height in the same
        // node. Audio/music tracks (a shared .mp4 every slide points at) do not. Requiring real
        // dimensions keeps us on the actual post media and off the background audio, and keeps
        // each carousel slide on its own image.
        long dim = a * b;
        if (dim > 0) {
            for (String u : urls) {
                boolean vid = u.split("\\?")[0].endsWith(".mp4");
                out.add(new Cand(u, vid, vid ? Math.max(bandwidth, dim) : dim));
            }
        }
        for (Field f : fields) {
            Object v = read(f, o);
            if (v == null || v instanceof String || v instanceof Number || v instanceof Boolean) continue;
            // Never follow a reference to another media object (parent, sibling, related): those
            // are separate items. Staying off them keeps each item's URLs its own. The item's own
            // image/video versions live under different (non-media) classes, which we still follow.
            if (rootClass.isInstance(v)) continue;
            collect(v, rootClass, depth + 1, seen, out);
        }
    }

    private static void findSameClassLists(Object o, Class<?> mediaClass, int depth, IdentityHashMap<Object, Boolean> seen, List<List<?>> out) {
        if (o == null || depth > MAX_DEPTH || out.size() > 8) return;
        if (o instanceof String || o instanceof Number || o instanceof Boolean) return;
        if (seen.put(o, Boolean.TRUE) != null) return;

        if (o instanceof List) {
            List<?> l = (List<?>) o;
            if (l.size() >= 2 && mediaClass.isInstance(l.get(0))) { out.add(l); return; }
            for (int i = 0; i < l.size() && i < 20; i++) findSameClassLists(l.get(i), mediaClass, depth + 1, seen, out);
            return;
        }
        String cn = o.getClass().getName();
        if (cn.startsWith("java.") || cn.startsWith("android.") || cn.startsWith("kotlin.")) return;
        for (Field f : declaredFields(o.getClass())) {
            Object v = read(f, o);
            if (v != null && !(v instanceof String) && !(v instanceof Number) && !(v instanceof Boolean))
                findSameClassLists(v, mediaClass, depth + 1, seen, out);
        }
    }

    private static Object read(Field f, Object o) {
        try { f.setAccessible(true); return f.get(o); } catch (Throwable t) { return null; }
    }

    private static Field[] declaredFields(Class<?> c) {
        List<Field> out = new ArrayList<>();
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            try { for (Field ff : k.getDeclaredFields()) out.add(ff); } catch (Throwable ignored) {}
        }
        return out.toArray(new Field[0]);
    }

    private static boolean isMediaUrl(String s) {
        if (s.length() < 12 || !s.startsWith("https://")) return false;
        String path = s.split("\\?")[0];
        boolean ext = path.endsWith(".mp4") || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".webp") || path.endsWith(".heic");
        return ext && (s.contains("cdninstagram") || s.contains("fbcdn"));
    }
}
