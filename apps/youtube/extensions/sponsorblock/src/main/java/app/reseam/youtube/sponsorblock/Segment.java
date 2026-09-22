// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock;

import android.graphics.Color;
import app.reseam.youtube.core.Settings;

/** Immutable API data. Playback decisions read current settings, including changes made mid-video. */
final class Segment implements SegmentTimeline.Range {
    enum Category {
        SPONSOR("sponsor", "Sponsor", "skip-once", "#CC00D400"),
        SELFPROMO("selfpromo", "Self promotion", "manual-skip", "#CCFFFF00"),
        INTERACTION("interaction", "Interaction reminder", "manual-skip", "#CCCC00FF"),
        HIGHLIGHT("poi_highlight", "Highlight", "manual-skip", "#CCFF1684"),
        INTRO("intro", "Intro", "manual-skip", "#CC00FFFF"),
        OUTRO("outro", "Outro", "manual-skip", "#CC0202ED"),
        PREVIEW("preview", "Preview", "manual-skip", "#CC008FD6"),
        HOOK("hook", "Hook", "ignore", "#CC395699"),
        FILLER("filler", "Filler", "ignore", "#CC7300FF"),
        MUSIC("music_offtopic", "Non-music section", "skip", "#CCFF9900");

        final String key, title, defaultMode, defaultColor;
        Category(String key, String title, String mode, String color) {
            this.key = key; this.title = title; defaultMode = mode; defaultColor = color;
        }
        String mode() { return Settings.getString("sb_" + key, defaultMode); }
        int color() {
            try { return Color.parseColor(Settings.getString("sb_" + key + "_color", defaultColor)); }
            catch (IllegalArgumentException ignored) { return Color.parseColor(defaultColor); }
        }
        static Category find(String key) {
            for (Category category : values()) if (category.key.equals(key)) return category;
            return null;
        }
    }
    final String uuid;
    final Category category;
    final long start, end;
    Segment(String uuid, Category category, long start, long end) {
        this.uuid = uuid; this.category = category; this.start = start; this.end = end;
    }
    boolean highlight() { return category == Category.HIGHLIGHT; }
    @Override public long start() { return start; }
    @Override public long end() { return end; }
    boolean ignored() { return "ignore".equals(category.mode()); }
    boolean automatic() { return category.mode().startsWith("skip"); }
}
