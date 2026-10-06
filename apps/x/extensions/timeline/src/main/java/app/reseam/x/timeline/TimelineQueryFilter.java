// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.timeline;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.reseam.runtime.settings.ReseamSettings;

/**
 * Keeps unwanted entries out of timelines. Every timeline is read from the timeline_entry table
 * (directly or through the TimelineView view). Promoted entries carry promoted_metadata;
 * recommendation modules and their children carry the module's server-assigned entry_id prefix.
 */
public final class TimelineQueryFilter {
    private static final class Toggle {
        final String key;
        final boolean defaultValue;

        Toggle(String key, boolean defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        boolean isOn() {
            return ReseamSettings.getBoolean(key, defaultValue);
        }
    }

    private static final Pattern TIMELINE_PREDICATE = Pattern.compile("WHERE (t\\.)?timeline_id = \\?");
    private static final String[] RECOMMENDATION_PREFIXES = {"who-to-follow-", "who-to-subscribe-", "community-to-join-"};
    private static volatile Toggle promoted;
    private static volatile Toggle recommendations;

    private TimelineQueryFilter() {}

    public static void hidePromoted(String key, boolean defaultValue) {
        promoted = new Toggle(key, defaultValue);
    }

    public static void hideRecommendations(String key, boolean defaultValue) {
        recommendations = new Toggle(key, defaultValue);
    }

    public static String rewrite(String sql) {
        if (sql == null || !readsTimeline(sql)) return sql;
        boolean hidePromoted = isOn(promoted);
        boolean hideRecommendations = isOn(recommendations);
        if (!hidePromoted && !hideRecommendations) return sql;
        Matcher predicate = TIMELINE_PREDICATE.matcher(sql);
        if (!predicate.find()) return sql;
        String alias = predicate.group(1) == null ? "" : predicate.group(1);
        StringBuilder where = new StringBuilder("WHERE ");
        if (hidePromoted) where.append(alias).append("promoted_metadata IS NULL AND ");
        if (hideRecommendations) {
            for (String prefix : RECOMMENDATION_PREFIXES) {
                where.append(alias).append("entry_id NOT LIKE '").append(prefix).append("%' AND ");
            }
        }
        return predicate.replaceFirst(where.append(alias).append("timeline_id = ?").toString());
    }

    private static boolean isOn(Toggle toggle) {
        return toggle != null && toggle.isOn();
    }

    private static boolean readsTimeline(String sql) {
        String lower = sql.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("select") && (lower.contains("from timeline_entry") || lower.contains("from timelineview"));
    }
}
