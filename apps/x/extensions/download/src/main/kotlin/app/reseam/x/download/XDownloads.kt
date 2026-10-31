// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
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
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

/** Returns true when it took over, so X skips its own download. */
@Export
fun save(context: Context?, uri: Any?, name: String?, mimeType: String?, headers: Map<*, *>?, internal: Boolean): Boolean {
    if (internal) return false
    context ?: return false
    val url = uri?.toString()?.takeIf { it.isNotEmpty() } ?: return false
    val mime = mimeType.orEmpty()
    val filename = name?.takeIf { it.isNotEmpty() } ?: filenameFrom(url, mime)
    val kind = if (mime.startsWith("video") || filename.endsWith(".mp4")) MediaKind.VIDEO else MediaKind.IMAGE
    val headerMap = headers.orEmpty().entries.mapNotNull { (k, v) -> (k as? String)?.let { it to v.toString() } }.toMap()
    val started =
        runCatching { context.saveMedia(XSettings.downloadFolder.value, url, filename, mime, kind, headerMap) }
            .onFailure { Logger.error(it) { "X media download failed for $url" } }
            .getOrDefault(false)
    if (started) context.showToast(XStrings.downloadStarted, Toast.LENGTH_SHORT)
    return started
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
