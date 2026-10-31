// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.instagram.download

import android.content.Context
import app.reseam.bundle.Logger
import app.reseam.bundle.saveMedia
import app.reseam.instagram.refs.children
import app.reseam.instagram.settings.DownloadSettings
import app.reseam.instagram.settings.InstagramStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

internal fun download(media: Any?, context: Context?, currentIndex: Int = -1) {
    if (media == null) return
    if (context == null) {
        Logger.error { "downloadMedia: no context available" }
        return
    }
    runCatching {
        val slides = children(media)?.takeIf { it.size > 1 }
        if (slides == null) {
            val started = downloadSingle(media, media, context, 0)
            showToast(
                context,
                if (started) InstagramStrings.downloadStarted
                else InstagramStrings.downloadMediaUrlMissing,
            )
        } else {
            handleCarousel(media, slides, context, currentIndex)
        }
    }
        .onFailure { error ->
            Logger.error(error) { "downloadMedia failed" }
            showToast(context, InstagramStrings.downloadFailed)
        }
}

private fun handleCarousel(
    parentMedia: Any,
    children: List<*>,
    context: Context,
    currentIndex: Int,
) {
    if (currentIndex !in children.indices) {
        downloadCarousel(parentMedia, children, context)
        return
    }
    showCarouselDialog(
        context,
        children.size,
        currentIndex,
        onCurrent = {
            val slideNumber = currentIndex + 1
            val started = downloadSingle(parentMedia, children[currentIndex], context, slideNumber)
            val message =
                if (started) InstagramStrings.downloadSlideStarted(slideNumber)
                else InstagramStrings.downloadSlideUrlMissing(slideNumber)
            showToast(context, message)
        },
        onAll = { downloadCarousel(parentMedia, children, context) },
    )
}

internal fun downloadStoryFromOwner(owner: Any?) {
    val reelItem = owner?.let(::storyOwnerReelItem)
    val context = owner?.let(::storyOwnerContext) as? Context
    if (reelItem == null) {
        showToast(context, InstagramStrings.downloadStoryMediaMissing)
        return
    }
    val media = reelItemMedia(reelItem)
    if (media != null) download(media, context)
    else showToast(context, InstagramStrings.downloadMediaUrlMissing)
}

private fun downloadCarousel(parentMedia: Any, children: List<*>, context: Context) {
    val count =
        children.withIndex().count { (index, child) ->
            val started = downloadSingle(parentMedia, child, context, index + 1)
            if (!started) showToast(context, InstagramStrings.downloadSlideUrlMissing(index + 1))
            started
        }
    showToast(
        context,
        if (count > 0) InstagramStrings.downloadCarouselStarted(count, children.size)
        else InstagramStrings.downloadCarouselUrlsMissing,
    )
}

private fun downloadSingle(metaMedia: Any, media: Any?, context: Context, index: Int): Boolean {
    if (media == null) return false
    val mediaUrl = bestMediaUrl(media)
    if (mediaUrl == null) {
        Logger.warn { "Could not extract URL from ${media.javaClass.name}" }
        return false
    }
    return enqueue(context, metaMedia, mediaUrl, index)
}

private fun buildFilename(media: Any, type: MediaType, index: Int): String {
    val user = username(media)?.takeIf { it.isNotEmpty() } ?: "unknown"
    val timestamp = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(Date())
    val uuid = UUID.randomUUID()
    val suffix = if (index > 1) "_$index" else ""
    return "${user}_${timestamp}_$uuid$suffix${type.extension}"
}

private fun enqueue(context: Context, media: Any, source: MediaUrl, index: Int): Boolean {
    val folder = DownloadSettings.folder.value.trim().ifEmpty { DownloadSettings.folder.default }
    return context.saveMedia(
        folder,
        source.url,
        buildFilename(media, source.type, index),
        source.type.mimeType,
        source.type.kind,
        notificationDescription = InstagramStrings.downloadNotificationDescription,
    )
}
