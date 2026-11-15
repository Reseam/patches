// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("RedditPosts")

package app.reseam.reddit.ailabels

import android.net.Uri
import app.reseam.ailabels.Post
import app.reseam.ailabels.PostImage
import app.reseam.ailabels.PostKind
import app.reseam.runtime.Export

private const val DOMAIN = "reddit"

// Pangram's extension checks at most the first 500 words of a Reddit post or comment.
private const val MAX_WORDS = 500

private val PHOTO_HOSTS = setOf("i.redd.it", "preview.redd.it")
private val PHOTO_FORMATS = setOf("jpg", "jpeg", "png", "webp")
private val WORD = Regex("\\S+")

// What Reddit's Link, a whole post, holds. The patch fills these in.

/** The post's `t3_` id. */
@Export external fun linkId(link: Any): String

@Export external fun linkTitle(link: Any): String

/** The post's markdown body, empty for a post without one. */
@Export external fun linkBody(link: Any): String?

@Export external fun linkPermalink(link: Any): String

@Export external fun linkAuthorId(link: Any): String?

@Export external fun linkAuthorName(link: Any): String?

/** 0 when unknown. */
@Export external fun linkCreatedSeconds(link: Any): Long

@Export external fun linkUrl(link: Any): String?

/** A gallery post's items, or null for any other post. */
@Export external fun galleryItems(link: Any): List<*>?

@Export external fun galleryItemUrl(item: Any): String?

@Export external fun promoted(link: Any): Boolean

// What Reddit's Comment holds. The patch fills these in.

/** False for the "more replies" entries a list of comments also holds. */
@Export external fun isComment(item: Any): Boolean

/** The comment's `t1_` id. */
@Export external fun commentId(comment: Any): String

/** The `t3_` id of the post the comment is on. */
@Export external fun commentPostId(comment: Any): String

@Export external fun commentBody(comment: Any): String?

@Export external fun commentAuthorId(comment: Any): String?

@Export external fun commentAuthorName(comment: Any): String?

/** 0 when unknown. */
@Export external fun commentCreatedSeconds(comment: Any): Long

internal fun postOf(link: Any): Post {
    val url = "https://www.reddit.com${linkPermalink(link)}"
    return Post(
        domain = DOMAIN,
        id = linkId(link),
        text = firstWords(linkBody(link).orEmpty()),
        url = url,
        authorId = linkAuthorId(link),
        authorName = linkAuthorName(link),
        createdAtMillis = millis(linkCreatedSeconds(link)),
        images = photoUrls(link).map { PostImage(DOMAIN, it, url) },
        title = linkTitle(link),
    )
}

internal fun postOfComment(comment: Any): Post {
    val postId = commentPostId(comment).removePrefix("t3_")
    val id = commentId(comment)
    return Post(
        domain = DOMAIN,
        id = id,
        text = firstWords(commentBody(comment).orEmpty()),
        url = "https://www.reddit.com/comments/$postId/_/${id.removePrefix("t1_")}/",
        authorId = commentAuthorId(comment),
        authorName = commentAuthorName(comment),
        createdAtMillis = millis(commentCreatedSeconds(comment)),
        images = emptyList(),
        kind = PostKind.Comment,
    )
}

/** Reddit serves a photo at i.redd.it and resized at preview.redd.it; both map to the i.redd.it link. */
internal fun standardPhotoUrl(url: String): String? {
    val uri = Uri.parse(url)
    val file = uri.lastPathSegment ?: return null
    if (uri.host !in PHOTO_HOSTS || file.substringAfterLast('.').lowercase() !in PHOTO_FORMATS) return null
    return "https://i.redd.it/$file"
}

private fun photoUrls(link: Any): List<String> =
    (listOfNotNull(linkUrl(link)) + galleryItems(link).orEmpty().mapNotNull { it?.let(::galleryItemUrl) })
        .mapNotNull(::standardPhotoUrl)
        .distinct()

private fun firstWords(text: String): String =
    WORD.findAll(text).elementAtOrNull(MAX_WORDS - 1)?.let { text.substring(0, it.range.last + 1) } ?: text

private fun millis(seconds: Long): Long? = seconds.takeIf { it > 0 }?.let { it * 1000 }
