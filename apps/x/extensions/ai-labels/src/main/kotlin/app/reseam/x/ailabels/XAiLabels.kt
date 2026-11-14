// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("XAiLabels")

package app.reseam.x.ailabels

import android.content.Context
import android.view.View
import app.reseam.ailabels.ImageLabelView
import app.reseam.ailabels.Palette
import app.reseam.ailabels.Post
import app.reseam.ailabels.PostImage
import app.reseam.ailabels.TextLabelView
import app.reseam.ailabels.labelsEnabled
import app.reseam.ailabels.prefetch
import app.reseam.ailabels.usePalette
import app.reseam.runtime.Export
import app.reseam.x.api.GraphqlObserver
import app.reseam.x.api.GraphqlResponse
import app.reseam.x.api.register
import app.reseam.x.posts.XPost
import app.reseam.x.posts.photoUrl
import app.reseam.x.posts.shownPosts
import app.reseam.x.posts.standardPhotoUrl
import app.reseam.x.posts.xPost
import java.util.concurrent.Executors

private const val DOMAIN = "x"

/** Reads X's current colors and hands them to [usePalette]. Filled in by the patch. */
@Export external fun applyPalette(colors: Any)

@Export
fun usePalette(sheet: Long, card: Long, text: Long, secondaryText: Long, border: Long, accent: Long, button: Long, onButton: Long, light: Boolean) =
    usePalette(Palette(argb(sheet), argb(card), argb(text), argb(secondaryText), argb(border), argb(accent), argb(button), argb(onButton), light))

/** A Compose sRGB color keeps its ARGB in the top 32 bits. */
private fun argb(color: Long): Int = (color ushr 32).toInt()

@Export fun textLabel(context: Context): View = TextLabelView(context)

@Export fun imageLabel(context: Context): View = ImageLabelView(context)

@Export fun bindText(view: View, model: Any?) = (view as TextLabelView).bind(model?.let { xPost(it).toPost() })

@Export fun bindImage(view: View, photo: Any?) = (view as ImageLabelView).bind(photo?.let(::photoUrl)?.let(::standardPhotoUrl))

@Export
fun prefetchTimelines() {
    register(TimelinePrefetch)
}

/** Starts checking the top of each page of posts as X receives it, so the labels are ready when it draws them. */
private object TimelinePrefetch : GraphqlObserver {
    // X loads many more posts than people scroll through; checking all of them would spend the detector's
    // daily limit on posts no one sees.
    private const val AHEAD = 10
    private val parser = Executors.newSingleThreadExecutor()

    override val isEnabled: Boolean get() = labelsEnabled

    override fun observe(response: GraphqlResponse) {
        parser.execute { response.json.shownPosts().take(AHEAD).forEach { prefetch(it.toPost()) } }
    }
}

private fun XPost.toPost(): Post {
    val url = "https://x.com/i/status/$id"
    return Post(
        domain = DOMAIN,
        id = id,
        text = text,
        url = url,
        authorId = authorId,
        authorName = authorName,
        createdAtMillis = postedAtMillis,
        images = photoUrls.map { PostImage(DOMAIN, it, url) },
    )
}
