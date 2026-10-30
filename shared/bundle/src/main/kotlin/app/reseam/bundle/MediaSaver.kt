// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import kotlin.concurrent.thread

/** Where the gallery looks for each kind of media. */
enum class MediaKind(val publicDir: String) {
    IMAGE(Environment.DIRECTORY_PICTURES),
    VIDEO(Environment.DIRECTORY_MOVIES),
}

/**
 * Saves [url] as [filename] where the gallery indexes it: into the SAF tree the user picked (a
 * `content://` [folder]), or the public [kind] folder with [folder] as a subdirectory. A SAF write
 * streams on a background thread; the rest go through [DownloadManager] with a completion
 * notification. Returns false only when nothing could be started.
 */
fun Context.saveMedia(
    folder: String,
    url: String,
    filename: String,
    mimeType: String,
    kind: MediaKind,
    headers: Map<String, String> = emptyMap(),
    notificationDescription: String? = null,
): Boolean {
    val target = folder.trim()
    if (target.startsWith("content://")) {
        streamToTree(Uri.parse(target), filename, mimeType, url, headers)
        return true
    }
    val manager = getSystemService(DownloadManager::class.java) ?: return false
    val relative = if (target.isEmpty()) filename else "$target/$filename"
    val request =
        DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(filename)
            notificationDescription?.let(::setDescription)
            setMimeType(mimeType)
            headers.forEach { (name, value) -> addRequestHeader(name, value) }
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(kind.publicDir, relative)
        }
    manager.enqueue(request)
    return true
}

private fun Context.streamToTree(
    tree: Uri,
    filename: String,
    mimeType: String,
    url: String,
    headers: Map<String, String>,
) {
    val resolver = applicationContext.contentResolver
    thread(name = "reseam-download") {
        runCatching { writeToTree(resolver, tree, filename, mimeType, url, headers) }
            .onFailure { Logger.error(it) { "SAF download failed for $filename" } }
    }
}

private fun writeToTree(
    resolver: ContentResolver,
    tree: Uri,
    filename: String,
    mimeType: String,
    url: String,
    headers: Map<String, String>,
) {
    val parent =
        DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    val file =
        DocumentsContract.createDocument(resolver, parent, mimeType, filename)
            ?: return Logger.error { "createDocument returned null for $filename" }
    val stream =
        resolver.openOutputStream(file)
            ?: return Logger.error { "openOutputStream returned null for $file" }
    // A failed stream close must not turn a completed download into a failure.
    stream.useReleasing({ runCatching { close() } }) { output ->
        openHttp(url) {
            requestMethod = "GET"
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
            connectTimeout = 15000
            readTimeout = 30000
            connect()
            if (responseCode in 200..299) {
                inputStream.useReleasing({ runCatching { close() } }) { input ->
                    input.copyTo(output, 16 * 1024)
                    output.flush()
                }
            } else {
                Logger.error { "HTTP $responseCode fetching $filename" }
            }
        }
    }
}
