// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("DownloadButton")

package app.reseam.x.download

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import java.util.WeakHashMap

/** A timeline post's photos, GIFs and videos, or null when it shows none. */
@Export external fun postMedia(post: Any?): List<*>?

/** A post options state's event sink: a lambda, compared by identity, that lives as long as the post. */
@Export external fun optionsKey(options: Any?): Any?

/** The 4K URL of a photo, or null for a GIF or video. */
@Export external fun photoUrl(media: Any): String?

/** X's pick of the best MP4 variant of a GIF or video, or null for a photo. */
@Export external fun bestVariant(media: Any): Any?

@Export external fun variantUrl(variant: Any): String?

@Export external fun variantType(variant: Any): String?

private lateinit var app: Context

// Options states are data classes rebuilt on every update, so posts are keyed by optionsKey.
private val posts = WeakHashMap<Any, Any>()

@Export
fun initButton(application: Application) {
    app = application
}

/** Remembers the post behind an options button, which only receives the options state. */
@Export
fun trackOptions(options: Any?, post: Any?) {
    val key = optionsKey(options)
    if (key != null && post != null) posts[key] = post
}

/** The media the download button next to an options button saves, or null when there is none. */
@Export
fun downloadTarget(options: Any?): Any? {
    if (!XSettings.downloadButton.value) return null
    return posts[optionsKey(options)]?.let(::postMedia)?.takeIf { it.isNotEmpty() }
}

@Export fun downloadLabel(): String = XStrings.downloadPost

/** The icon's size in dp: larger than the 16 dp menu dots, whose glyph is taller than this one. */
@Export fun iconSize(): Float = 20f

/** Space in dp on each side of the icon, so taps meant for its neighbours do not land on it. */
@Export fun iconSpacing(): Float = 8f

/** Saves every photo, GIF and video in [media], a value from [downloadTarget]. */
@Export
fun saveAll(media: Any?) {
    (media as? List<*>).orEmpty().filterNotNull().forEach { item ->
        val photo = photoUrl(item)
        if (photo != null) {
            val format = Uri.parse(photo).getQueryParameter("format").orEmpty()
            save(app, photo, null, MimeTypeMap.getSingleton().getMimeTypeFromExtension(format), null, false)
        } else {
            bestVariant(item)?.let { save(app, variantUrl(it), null, variantType(it), null, false) }
        }
    }
}
