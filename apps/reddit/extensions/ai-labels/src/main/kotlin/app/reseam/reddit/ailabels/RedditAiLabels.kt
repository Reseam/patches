// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("RedditAiLabels")

package app.reseam.reddit.ailabels

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.View
import app.reseam.ailabels.CheckEvents
import app.reseam.ailabels.ImageLabelView
import app.reseam.ailabels.Post
import app.reseam.ailabels.TextLabelView
import app.reseam.ailabels.prefetch
import app.reseam.runtime.Export
import java.util.WeakHashMap

// Reddit loads posts and comments a page at a time; checking all of them would spend the detector's daily limit
// on ones no one scrolls to.
private const val AHEAD = 10

/** Posts and comments Reddit loaded, by their `t3_` or `t1_` id. */
private val loaded = LruCache<String, Post>(1_000)

/** Labels drawn before their post or comment loaded, and the id each waits for. */
private val waiting = WeakHashMap<TextLabelView, String>()

private val main = Handler(Looper.getMainLooper())
private var composingPost: String? = null

@Export fun textLabel(context: Context): View = TextLabelView(context)

@Export fun imageLabel(context: Context): View = ImageLabelView(context)

/** The post whose header is composing, for the row it draws, which is not told its post. */
@Export
fun enterPost(id: String?) {
    // A post page opened from a link names its post without the `t3_` prefix.
    composingPost = id?.let { "t3_" + it.removePrefix("t3_") }
}

@Export
fun exitPost() {
    composingPost = null
}

@Export fun currentPost(): String? = composingPost

/** A null [id] is a header row recomposing on its own; the view keeps what it shows. */
@Export
fun bindText(view: View, id: String?) {
    id ?: return
    val label = view as TextLabelView
    val item = loaded[id]
    if (item == null) waiting[label] = id else waiting.remove(label)
    label.bind(item)
}

@Export fun bindImage(view: View, url: String?) = (view as ImageLabelView).bind(url?.let(::standardPhotoUrl))

/** Every batch of whole posts Reddit loads: a feed page it keeps for opening posts, or the post it opens. */
@Export fun onLinks(links: List<*>) = onLoaded(links.filterNotNull().filterNot(::promoted).map(::postOf))

/** Every batch of comments Reddit loads for a post. */
@Export fun onComments(items: List<*>) = onLoaded(items.filterNotNull().filter(::isComment).map(::postOfComment))

private fun onLoaded(batch: List<Post>) {
    batch.forEach { loaded.put(it.id, it) }
    batch.take(AHEAD).forEach(::prefetch)
    main.post {
        val ready = waiting.entries.mapNotNull { (view, id) -> loaded[id]?.let { view to it } }
        ready.forEach { (view, item) ->
            waiting.remove(view)
            view.bind(item)
        }
        // Binding registers the posts' images; image labels drawn before that look again.
        if (ready.isNotEmpty()) CheckEvents.notifyChanged()
    }
}
