// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("FollowingFeed")

package app.reseam.instagram.feed

import app.reseam.runtime.Export

private const val PAGINATION_SOURCE = "pagination_source"
private const val SUGGESTED_POSTS = "feed_recs"
private const val OLDER_POSTS = "past_posts"

@Export
fun following(params: Map<*, *>?): MutableMap<Any?, Any?> =
    HashMap<Any?, Any?>(params.orEmpty()).apply {
        val source = this[PAGINATION_SOURCE]
        if (source == null || SUGGESTED_POSTS == source) this[PAGINATION_SOURCE] = "following"
    }

@Export external fun groupId(group: Any): String?

/** The groups of the end-of-feed group set, without the suggested posts. */
@Export
fun withoutSuggestedPosts(groups: List<Any>?): List<Any>? = groups?.filterNot { groupId(it) == SUGGESTED_POSTS }

@Export
fun activeGroup(active: String?): String? = if (active == SUGGESTED_POSTS) OLDER_POSTS else active
