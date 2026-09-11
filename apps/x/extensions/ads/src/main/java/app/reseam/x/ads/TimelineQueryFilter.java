// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.x.ads;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.reseam.runtime.settings.ReseamSettings;

/**
 * Keeps promoted entries out of timelines. Every timeline is read from the timeline_entry table
 * (directly or through the TimelineView view) and promoted entries carry promoted_metadata.
 */
public final class TimelineQueryFilter {
    private static final Pattern TIMELINE_PREDICATE = Pattern.compile("WHERE (t\\.)?timeline_id = \\?");
    private static volatile String toggleKey;
    private static volatile boolean toggleDefault;

    private TimelineQueryFilter() {}

    public static void init(String key, boolean defaultValue) {
        toggleKey = key;
        toggleDefault = defaultValue;
    }

    public static String rewrite(String sql) {
        if (sql == null || !readsTimeline(sql) || !ReseamSettings.getBoolean(toggleKey, toggleDefault)) return sql;
        Matcher predicate = TIMELINE_PREDICATE.matcher(sql);
        if (!predicate.find()) return sql;
        String alias = predicate.group(1) == null ? "" : predicate.group(1);
        return predicate.replaceFirst("WHERE " + alias + "promoted_metadata IS NULL AND " + alias + "timeline_id = ?");
    }

    private static boolean readsTimeline(String sql) {
        String lower = sql.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("select") && (lower.contains("from timeline_entry") || lower.contains("from timelineview"));
    }
}
