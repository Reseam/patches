// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("PinDownloads")

package app.reseam.pinterest.download

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import app.reseam.bundle.MediaKind
import app.reseam.bundle.ReseamPatches
import app.reseam.bundle.openHttp
import app.reseam.bundle.resumedActivity
import app.reseam.bundle.saveMedia
import app.reseam.bundle.showToast
import app.reseam.bundle.trackResumedActivity
import app.reseam.pinterest.refs.carousel
import app.reseam.pinterest.refs.carouselSlots
import app.reseam.pinterest.refs.id
import app.reseam.pinterest.refs.imageUrl
import app.reseam.pinterest.refs.imageWidth
import app.reseam.pinterest.refs.images
import app.reseam.pinterest.refs.slotImages
import app.reseam.pinterest.refs.slotVideos
import app.reseam.pinterest.refs.story
import app.reseam.pinterest.refs.storyBlockImage
import app.reseam.pinterest.refs.storyBlockVideo
import app.reseam.pinterest.refs.storyBlocks
import app.reseam.pinterest.refs.storyImages
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
import kotlin.concurrent.thread

private const val ORIGINAL_SIZE = "orig"

// Pinterest's CDN keeps each image's original under the path of its sized copies.
private val sizedImage = Regex("""(https://i\.pinimg\.com/)(?!originals/)[^/]+(/.+)""")

internal lateinit var app: Context

private class Media(val url: String, val video: Boolean)

@Export
fun init(application: Application) {
    app = application
    trackResumedActivity(application)
}

@Export
fun withDownload(actions: List<Any?>, download: Any, copyLink: Any): List<Any?> =
    if (download in actions) actions
    else actions.toMutableList().apply { add(indexOf(copyLink).takeIf { it >= 0 }?.inc() ?: size, download) }

@Export
fun save(pin: Any) {
    val media = media(pin)
    val activity = resumedActivity()
    if (media.size < 2 || activity == null || !DownloadSettings.askWhichMedia.value) saveAll(pin, media)
    else choose(activity, pin, media)
}

/** Makes network requests, so callers run it off the main thread. */
internal fun enqueue(pin: Any): Boolean = media(pin).let { saveItems(pin, it, it.indices) }

private fun saveAll(pin: Any, media: List<Media>) = saveInBackground(pin, media, media.indices)

private fun choose(activity: Activity, pin: Any, media: List<Media>) {
    val ui = ReseamPatches.bundle().ui(activity)
    val list = ui.itemGroup()
    var dialog: Dialog? = null
    media.forEachIndexed { index, item ->
        val title = if (item.video) PinterestStrings.saveMediaVideo(index + 1) else PinterestStrings.saveMediaImage(index + 1)
        list.add(
            ui.listItem(title).onClick {
                dialog?.dismiss()
                saveInBackground(pin, media, listOf(index))
            },
        )
    }
    dialog = ui.dialog()
        .title(PinterestStrings.saveMediaChoose)
        .view(list)
        .dismissAction(activity.getString(android.R.string.cancel))
        .action(PinterestStrings.saveMediaAll) { saveAll(pin, media) }
        .show()
}

private fun toast(started: Boolean) = app.showToast(if (started) PinterestStrings.downloadStarted else PinterestStrings.downloadUnavailable)

private fun saveInBackground(pin: Any, media: List<Media>, indices: Iterable<Int>) {
    thread(name = "reseam-pin-download") { toast(saveItems(pin, media, indices)) }
}

// Each file keeps its position in the pin, so saving one item names it the same as saving all.
private fun saveItems(pin: Any, media: List<Media>, indices: Iterable<Int>): Boolean =
    indices.count { index ->
        val item = media[index]
        app.save(item.original(), fileName(pin, extension(item.url), index.takeIf { media.size > 1 }))
    } > 0

// Carousel slots come only in small sizes, so a sized image is swapped for its original when the CDN has one.
private fun Media.original(): Media {
    if (video) return this
    val match = sizedImage.matchEntire(url) ?: return this
    val original = match.groupValues[1] + "originals" + match.groupValues[2]
    return if (exists(original)) image(original) else this
}

private fun exists(url: String): Boolean =
    runCatching {
        openHttp(url) {
            requestMethod = "HEAD"
            connectTimeout = 10_000
            readTimeout = 10_000
            responseCode in 200..299
        }
    }.getOrDefault(false)

private fun Context.save(media: Media, name: String): Boolean {
    val extension = extension(media.url)
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
    return saveMedia(DownloadSettings.downloadFolder.value, media.url, name, mimeType, if (media.video) MediaKind.VIDEO else MediaKind.IMAGE)
}

private fun extension(url: String): String = Uri.parse(url).lastPathSegment?.substringAfterLast('.', "jpg") ?: "jpg"

private fun fileName(pin: Any, extension: String, page: Int?): String =
    "pinterest_${id(pin)}${page?.let { "_${it + 1}" }.orEmpty()}.$extension"

// A single image saves the pin's own original; several images or any video save each slot or story page.
private fun media(pin: Any): List<Media> =
    listOfNotNull(videoList(pin)?.let(::videos)?.let(::bestMp4)?.let(::video))
        .ifEmpty { carouselMedia(pin).takeIf { it.size > 1 }.orEmpty() }
        .ifEmpty { storyMedia(pin).takeIf { items -> items.size > 1 || items.any { it.video } }.orEmpty() }
        .ifEmpty { listOfNotNull(images(pin)?.let(::bestImage)?.let(::image)) }

private fun carouselMedia(pin: Any): List<Media> =
    carousel(pin)?.let(::carouselSlots).orEmpty().mapNotNull { slot ->
        slotVideos(slot)?.let(::videos)?.let(::bestMp4)?.let(::video) ?: slotImages(slot)?.let(::bestImage)?.let(::image)
    }

private fun storyMedia(pin: Any): List<Media> =
    story(pin)?.let { storyPages(it) ?: storyPagesPreview(it) }.orEmpty()
        .flatMap { storyBlocks(it).orEmpty() }
        .mapNotNull { block ->
            storyBlockVideo(block)?.let(::storyVideos)?.let(::bestMp4)?.let(::video)
                ?: storyBlockImage(block)?.let(::storyImages)?.let(::bestImage)?.let(::image)
        }

private fun video(url: String) = Media(url, video = true)

private fun image(url: String) = Media(url, video = false)

// Streaming-only variants (HLS) have no single file to save.
private fun bestMp4(variants: Map<String, Any>): String? =
    variants.values
        .filter { videoUrl(it)?.endsWith(".mp4") == true }
        .maxByOrNull { videoBitrate(it)?.toLong() ?: 0 }
        ?.let(::videoUrl)

private fun bestImage(sizes: Map<String, Any>): String? =
    (sizes[ORIGINAL_SIZE] ?: sizes.values.maxByOrNull { imageWidth(it) ?: 0.0 })?.let(::imageUrl)
