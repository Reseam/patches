// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.ailabels

internal val WHITESPACE = Regex("\\s+")

/** An image in a post. [pageUrl] is the post's link. */
class PostImage(val domain: String, val url: String, val pageUrl: String)

/** A post as an app shows it. [domain] names the platform, such as `x` or `reddit`. */
class Post(
    val domain: String,
    val id: String,
    val text: String,
    val url: String,
    val authorId: String?,
    val authorName: String?,
    val createdAtMillis: Long?,
    val images: List<PostImage>,
) {
    val wordCount: Int by lazy { text.trim().split(WHITESPACE).count { it.isNotEmpty() } }
}
