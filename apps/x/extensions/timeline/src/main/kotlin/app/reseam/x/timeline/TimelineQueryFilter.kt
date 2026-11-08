// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("TimelineQueryFilter")

package app.reseam.x.timeline

import app.reseam.bundle.ReseamPatches
import app.reseam.runtime.Export
import app.reseam.runtime.ToggleValue
import java.util.Locale
import java.util.regex.Pattern

private val timelinePredicate = Pattern.compile("WHERE (t\\.)?timeline_id = \\?")
private val recommendationPrefixes =
    listOf("who-to-follow-", "who-to-subscribe-", "community-to-join-")
// The filter is shared, so each patch registers its own condition: one left out of the build adds none.
@Volatile private var promoted: ToggleValue? = null
@Volatile private var recommendations: ToggleValue? = null
@Volatile private var stories: ToggleValue? = null
@Volatile private var exploreTrends: ToggleValue? = null

@Export
fun hidePromoted(key: String, defaultValue: Boolean) {
    promoted = ToggleValue(ReseamPatches.NAME, key, defaultValue)
}

@Export
fun hideRecommendations(key: String, defaultValue: Boolean) {
    recommendations = ToggleValue(ReseamPatches.NAME, key, defaultValue)
}

@Export
fun hideStories(key: String, defaultValue: Boolean) {
    stories = ToggleValue(ReseamPatches.NAME, key, defaultValue)
}

@Export
fun hideExploreTrends(key: String, defaultValue: Boolean) {
    exploreTrends = ToggleValue(ReseamPatches.NAME, key, defaultValue)
}

@Export
fun rewrite(sql: String?): String? {
    if (sql == null || !readsTimeline(sql)) return sql
    val hidePromoted = promoted?.value == true
    val hideRecommendations = recommendations?.value == true
    val hideStories = stories?.value == true
    val hideExploreTrends = exploreTrends?.value == true
    if (!hidePromoted && !hideRecommendations && !hideStories && !hideExploreTrends) return sql
    val predicate = timelinePredicate.matcher(sql)
    if (!predicate.find()) return sql
    val alias = predicate.group(1) ?: ""
    val conditions = buildList {
        if (hidePromoted) add("${alias}promoted_metadata IS NULL")
        if (hideRecommendations) {
            addAll(recommendationPrefixes.map { "${alias}entry_id NOT LIKE '$it%'" })
        }
        // A story module's items carry the module's entry id as their prefix.
        if (hideStories) add("${alias}entry_id NOT LIKE 'stories-%'")
        if (hideExploreTrends) {
            add("NOT (${alias}timeline_id LIKE 'EXPLORE_FOR_YOU%' AND ${alias}entry_id LIKE 'trend-%')")
        }
        add("${alias}timeline_id = ?")
    }
    return predicate.replaceFirst("WHERE ${conditions.joinToString(" AND ")}")
}

private fun readsTimeline(sql: String): Boolean {
    val lower = sql.trim().lowercase(Locale.ROOT)
    return lower.startsWith("select") &&
        listOf("from timeline_entry", "from timelineview").any { it in lower }
}
