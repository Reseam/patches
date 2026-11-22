// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("XPosts")

package app.reseam.x.posts

import app.reseam.runtime.Export

// What a post holds, read from X's Compose post model. The patch fills these in.

@Export external fun postId(post: Any): Long

/** The whole text, including what a long post shows only after "Show more". */
@Export external fun postText(post: Any): String?

/** 0 when unknown. */
@Export external fun authorId(post: Any): Long

@Export external fun authorName(post: Any): String?

/** The author's handle, without the @. */
@Export external fun authorScreenName(post: Any): String?

/** 0 when unknown. */
@Export external fun postedAtSeconds(post: Any): Long

/** The post's photos, GIFs and videos, or null for a post without any. */
@Export external fun postMedia(post: Any): List<*>?

/** The photo's link, or null for a GIF or video. */
@Export external fun photoUrl(media: Any): String?
