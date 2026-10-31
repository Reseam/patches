// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PinDownloads")

package app.reseam.pinterest.download

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import app.reseam.bundle.MediaKind
import app.reseam.bundle.saveMedia
import app.reseam.bundle.showToast
import app.reseam.pinterest.refs.id
import app.reseam.pinterest.refs.imageUrl
import app.reseam.pinterest.refs.imageWidth
import app.reseam.pinterest.refs.images
import app.reseam.pinterest.refs.story
import app.reseam.pinterest.refs.storyBlockVideo
import app.reseam.pinterest.refs.storyBlocks
import app.reseam.pinterest.refs.storyPages
import app.reseam.pinterest.refs.storyPagesPreview
import app.reseam.pinterest.refs.storyVideos
import app.reseam.pinterest.refs.videoBitrate
import app.reseam.pinterest.refs.videoList
import app.reseam.pinterest.refs.videoUrl
import app.reseam.pinterest.refs.videos
import app.reseam.pinterest.settings.DownloadSettings
import app.reseam.pinterest.settings.PinterestStrings
import app.reseam.runtime.Export

private const val ORIGINAL_SIZE = "orig"

internal lateinit var app: Context

@Export
fun init(context: Context) {
    app = context.applicationContext
}

@Export
fun withDownload(actions: List<Any?>, download: Any, copyLink: Any): List<Any?> =
    if (download in actions) actions
    else actions.toMutableList().apply { add(indexOf(copyLink).takeIf { it >= 0 }?.inc() ?: size, download) }

@Export
fun save(pin: Any) = app.showToast(if (enqueue(pin)) PinterestStrings.downloadStarted else PinterestStrings.downloadUnavailable)

internal fun enqueue(pin: Any): Boolean {
    val urls = mediaUrls(pin)
    return urls.withIndex().count { (page, url) -> app.save(url, fileName(pin, extension(url), page.takeIf { urls.size > 1 })) } > 0
}

private fun Context.save(url: String, name: String): Boolean {
    val extension = extension(url)
    val kind = if (extension == "mp4") MediaKind.VIDEO else MediaKind.IMAGE
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
    return saveMedia(DownloadSettings.downloadFolder.value, url, name, mimeType, kind)
}

private fun extension(url: String): String = Uri.parse(url).lastPathSegment?.substringAfterLast('.', "jpg") ?: "jpg"

private fun fileName(pin: Any, extension: String, page: Int?): String =
    "pinterest_${id(pin)}${page?.let { "_${it + 1}" }.orEmpty()}.$extension"

private fun mediaUrls(pin: Any): List<String> = videoUrls(pin).ifEmpty { listOfNotNull(originalImageUrl(pin)) }

private fun videoUrls(pin: Any): List<String> =
    listOfNotNull(videoList(pin)?.let(::videos)?.let(::bestMp4)).ifEmpty {
        story(pin)?.let { storyPages(it) ?: storyPagesPreview(it) }.orEmpty()
            .flatMap { storyBlocks(it).orEmpty() }
            .mapNotNull { storyBlockVideo(it)?.let(::storyVideos)?.let(::bestMp4) }
    }

// Streaming-only variants (HLS) have no single file to save.
private fun bestMp4(variants: Map<String, Any>): String? =
    variants.values
        .filter { videoUrl(it)?.endsWith(".mp4") == true }
        .maxByOrNull { videoBitrate(it)?.toLong() ?: 0 }
        ?.let(::videoUrl)

private fun originalImageUrl(pin: Any): String? =
    images(pin)?.let { sizes -> sizes[ORIGINAL_SIZE] ?: sizes.values.maxByOrNull { imageWidth(it) ?: 0.0 } }?.let(::imageUrl)
