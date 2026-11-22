// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ShownPosts")

package app.reseam.x.download

import app.reseam.runtime.Export
import app.reseam.x.posts.postId
import app.reseam.x.posts.postMedia
import java.util.WeakHashMap

@Export external fun optionsKey(options: Any?): Any?

private const val RECENT_POSTS = 200

// Options states are data classes rebuilt on every update; their event sink is one identity per post
// while it is composed, so it only holds for the composition that tracks it.
private val composed = WeakHashMap<Any, Any>()

// Saves happen after composition, from the post or the media viewer opened from it.
private val recent = object : LinkedHashMap<Long, Any>(RECENT_POSTS, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Any>) = size > RECENT_POSTS
}

/** A drawn post and the 1-based position of one of its media. */
internal class ShownMedia(val post: Any, val position: Int)

@Export
fun track(options: Any?, post: Any?) {
    post ?: return
    val key = optionsKey(options)
    synchronized(recent) {
        if (key != null) composed[key] = post
        recent[postId(post)] = post
    }
}

internal fun composedPost(options: Any?): Any? = optionsKey(options)?.let { synchronized(recent) { composed[it] } }

/** The recently drawn post that holds the media at [url]. */
internal fun shownMedia(url: String): ShownMedia? {
    val key = mediaKey(url)
    val posts = synchronized(recent) { recent.values.reversed() }
    for (post in posts) {
        val index = postMedia(post).orEmpty().indexOfFirst { media -> media?.let(::mediaUrl)?.let(::mediaKey) == key }
        if (index >= 0) return ShownMedia(post, index + 1)
    }
    return null
}
