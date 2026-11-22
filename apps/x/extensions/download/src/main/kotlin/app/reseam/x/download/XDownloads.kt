// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("XDownloads")

package app.reseam.x.download

import android.content.Context
import android.widget.Toast
import app.reseam.bundle.Logger
import app.reseam.bundle.MediaKind
import app.reseam.bundle.saveMedia
import app.reseam.bundle.showToast
import app.reseam.runtime.Export
import app.reseam.x.posts.xPost
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Returns true when it took over, so X skips its own download. */
@Export
fun save(context: Context?, uri: Any?, name: String?, mimeType: String?, headers: Map<*, *>?, internal: Boolean): Boolean {
    if (internal) return false
    context ?: return false
    val url = uri?.toString()?.takeIf { it.isNotEmpty() } ?: return false
    val mime = mimeType.orEmpty()
    val filename = postFileName(url, mime) ?: name?.takeIf { it.isNotEmpty() } ?: filenameFrom(url, mime)
    val kind = if (mime.startsWith("video") || filename.endsWith(".mp4")) MediaKind.VIDEO else MediaKind.IMAGE
    val headerMap = headers.orEmpty().entries.mapNotNull { (k, v) -> (k as? String)?.let { it to v.toString() } }.toMap()
    val started =
        runCatching { context.saveMedia(XSettings.downloadFolder.value, url, filename, mime, kind, headerMap) }
            .onFailure { Logger.error(it) { "X media download failed for $url" } }
            .getOrDefault(false)
    if (started) context.showToast(XStrings.downloadStarted, Toast.LENGTH_SHORT)
    return started
}

private val placeholder = Regex("\\{([a-z_]+)\\}")
private val unsafeInFileName = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
private val timeFormat = DateTimeFormatter.ofPattern("HH-mm-ss", Locale.ROOT)
private const val MAX_NAME_LENGTH = 80

/** The File name pattern filled in for the media at [url]; null without a pattern or when its post was not drawn lately. */
private fun postFileName(url: String, mime: String): String? {
    val pattern = XSettings.downloadFileName.value.trim().ifEmpty { return null }
    val shown = shownMedia(url) ?: return null
    val post = xPost(shown.post)
    val posted = post.postedAtMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
    val values = mapOf(
        "username" to post.authorScreenName,
        "name" to post.authorName,
        "post_id" to post.id,
        "n" to shown.position.toString(),
        "date" to posted?.format(dateFormat),
        "time" to posted?.format(timeFormat),
        "media_id" to mediaId(url),
    )
    val name = placeholder.replace(pattern) { token ->
        val key = token.groupValues[1]
        if (key in values) values[key].orEmpty() else token.value
    }
    val safe = name.replace(unsafeInFileName, "_").trim().take(MAX_NAME_LENGTH).trim().ifEmpty { return null }
    return "$safe.${filenameFrom(url, mime).substringAfterLast('.')}"
}

private fun filenameFrom(url: String, mime: String): String {
    val base = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
    if (base.contains('.')) return base
    val extension =
        when {
            "mp4" in mime -> "mp4"
            "jpeg" in mime || "jpg" in mime -> "jpg"
            "png" in mime -> "png"
            "webp" in mime -> "webp"
            "gif" in mime -> "gif"
            else -> "bin"
        }
    return "${base.ifEmpty { "x_${System.currentTimeMillis()}" }}.$extension"
}
