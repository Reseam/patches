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
private class Filter(val conditions: (alias: String) -> List<String>) {
    @Volatile var toggle: ToggleValue? = null
    val enabled get() = toggle?.value == true
}

private val promoted = Filter { listOf("${it}promoted_metadata IS NULL") }
private val recommendations = Filter { t -> recommendationPrefixes.map { "${t}entry_id NOT LIKE '$it%'" } }
// A module's items carry the module's entry id as their prefix.
private val videosForYou = Filter { listOf("${it}entry_id NOT LIKE 'video-carousel-%'") }
private val stories = Filter { listOf("${it}entry_id NOT LIKE 'stories-%'") }
private val discoverMore = Filter { listOf("${it}entry_id NOT LIKE 'tweetdetailrelatedtweets-%'") }
private val exploreTrends = Filter {
    listOf("NOT (${it}timeline_id LIKE 'EXPLORE_FOR_YOU%' AND ${it}entry_id LIKE 'trend-%')")
}
// Posts For You starts as a module of posts and continues as every later page, so the bottom cursor
// that loads those pages goes too.
private val explorePosts = Filter {
    listOf(
        "NOT (${it}timeline_id LIKE 'EXPLORE_FOR_YOU%' AND " +
            "(${it}entry_id LIKE 'tweet-%' OR ${it}entry_id LIKE 'cursor-bottom-%'))",
    )
}
private val filters = listOf(promoted, recommendations, videosForYou, stories, discoverMore, exploreTrends, explorePosts)

@Export
fun hidePromoted(key: String, defaultValue: Boolean) = promoted.register(key, defaultValue)

@Export
fun hideRecommendations(key: String, defaultValue: Boolean) = recommendations.register(key, defaultValue)

@Export
fun hideVideosForYou(key: String, defaultValue: Boolean) = videosForYou.register(key, defaultValue)

@Export
fun hideStories(key: String, defaultValue: Boolean) = stories.register(key, defaultValue)

@Export
fun hideDiscoverMore(key: String, defaultValue: Boolean) = discoverMore.register(key, defaultValue)

@Export
fun hideExploreTrends(key: String, defaultValue: Boolean) = exploreTrends.register(key, defaultValue)

@Export
fun hideExplorePosts(key: String, defaultValue: Boolean) = explorePosts.register(key, defaultValue)

@Export
fun rewrite(sql: String?): String? {
    if (sql == null || !readsTimeline(sql)) return sql
    val active = filters.filter { it.enabled }
    if (active.isEmpty()) return sql
    val predicate = timelinePredicate.matcher(sql)
    if (!predicate.find()) return sql
    val alias = predicate.group(1) ?: ""
    val conditions = active.flatMap { it.conditions(alias) } + "${alias}timeline_id = ?"
    return predicate.replaceFirst("WHERE ${conditions.joinToString(" AND ")}")
}

private fun Filter.register(key: String, defaultValue: Boolean) {
    toggle = ToggleValue(ReseamPatches.NAME, key, defaultValue)
}

private fun readsTimeline(sql: String): Boolean {
    val lower = sql.trim().lowercase(Locale.ROOT)
    return lower.startsWith("select") &&
        listOf("from timeline_entry", "from timelineview").any { it in lower }
}
