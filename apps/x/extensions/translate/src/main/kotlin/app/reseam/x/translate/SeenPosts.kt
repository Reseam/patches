// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

// X's Translate button sends only a post's id, so the text comes from the timelines it was shown in.
internal object SeenPosts {
    private const val MAX_POSTS = 2_000

    private val posts =
        object : LinkedHashMap<String, PostText>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PostText>) = size > MAX_POSTS
        }

    @Synchronized
    fun remember(id: String, text: PostText) {
        posts[id] = text
    }

    @Synchronized
    operator fun get(id: String): PostText? = posts[id]
}
