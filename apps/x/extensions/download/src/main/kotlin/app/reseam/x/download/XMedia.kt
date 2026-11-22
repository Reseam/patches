// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("XMedia")

package app.reseam.x.download

import android.net.Uri
import app.reseam.runtime.Export
import app.reseam.x.posts.photoUrl
import app.reseam.x.posts.standardPhotoUrl

@Export external fun isGif(media: Any): Boolean

@Export external fun bestVariant(media: Any): Any?

@Export external fun variantUrl(variant: Any): String?

@Export external fun variantType(variant: Any): String?

/** A link to the photo, or to the best video or GIF variant. */
internal fun mediaUrl(media: Any): String? = photoUrl(media) ?: bestVariant(media)?.let(::variantUrl)

/**
 * The same for every link X serves one photo or video at: a photo's standard link, or a video's
 * first two path segments (ext_tw_video/<id>, amplify_video/<id>, tweet_video/<file>).
 */
internal fun mediaKey(url: String): String {
    val uri = Uri.parse(url)
    return if (uri.host == "pbs.twimg.com") standardPhotoUrl(url) else uri.pathSegments.take(2).joinToString("/")
}

/** X's id for the media: a photo's file name, or a video's id. */
internal fun mediaId(url: String): String {
    val uri = Uri.parse(url)
    val segment = if (uri.host == "pbs.twimg.com") uri.lastPathSegment else uri.pathSegments.getOrNull(1)
    return segment.orEmpty().substringBefore('.')
}
