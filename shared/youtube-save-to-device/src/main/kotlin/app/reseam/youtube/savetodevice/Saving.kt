// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.savetodevice

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.system.ErrnoException
import android.system.OsConstants
import app.reseam.bundle.Logger
import app.reseam.youtubesavetodevice.settings.SaveToDeviceStrings
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.Executors

internal sealed class SaveTarget(val label: String) {
    class Video(val videoId: String) : SaveTarget(videoId)

    class Playlist(val playlistId: String) : SaveTarget(playlistId)
}

private val executor = Executors.newSingleThreadExecutor { Thread(it, "reseam-save-to-device") }
private val UNSAFE_NAME = Regex("""[\\/:*?"<>|\p{Cntrl}]""")
private const val MAX_NAME = 120

/** Saves on one background thread, in the order asked, into the save folder [tree]. */
internal fun enqueue(context: Context, tree: Uri, target: SaveTarget) {
    val notifications = SaveNotifications(context)
    executor.execute {
        // An exception leaving an executor thread would end the app's process.
        val id = notifications.newId()
        try {
            val store = OfflineStore(context)
            when (target) {
                is SaveTarget.Video -> saveVideo(context, store, notifications, id, tree, target.videoId)
                is SaveTarget.Playlist -> savePlaylist(context, store, notifications, id, tree, target.playlistId)
            }
        } catch (exception: Exception) {
            Logger.error(exception) { "Could not read the offline library" }
            notifications.failed(id, target.label, null)
        }
    }
}

private fun saveVideo(
    context: Context,
    store: OfflineStore,
    notifications: SaveNotifications,
    id: Int,
    tree: Uri,
    videoId: String,
) {
    val video = store.video(videoId) ?: return notifications.failed(id, videoId, SaveToDeviceStrings.saveFailedIncomplete)
    val document = save(context, store, notifications, id, video, tree.root(), null) ?: return
    notifications.saved(id, video.title, document, context.contentResolver.getType(document))
}

private fun savePlaylist(
    context: Context,
    store: OfflineStore,
    notifications: SaveNotifications,
    id: Int,
    tree: Uri,
    playlistId: String,
) {
    val playlist = store.playlist(playlistId)
    if (playlist == null || playlist.videos.isEmpty()) {
        notifications.failed(id, playlistId, SaveToDeviceStrings.saveFailedIncomplete)
        return
    }
    val folder =
        try {
            context.contentResolver.childFolder(tree, tree.root(), fileName(playlist.title))
        } catch (exception: Exception) {
            Logger.error(exception) { "Could not create the folder for ${playlist.title}" }
            notifications.failed(id, playlist.title, SaveToDeviceStrings.saveFailedFolder)
            return
        }
    val total = playlist.videos.size
    var saved = 0
    playlist.videos.forEachIndexed { index, video ->
        notifications.progress(id, playlist.title, SaveToDeviceStrings.savingProgress(index + 1, total), index.toFloat() / total)
        if (save(context, store, notifications, notifications.newId(), video, folder, index + 1 to total) != null) saved++
    }
    notifications.savedFolder(id, SaveToDeviceStrings.savedCount(saved), folder)
}

/** The saved document, or null after posting why it failed. */
private fun save(
    context: Context,
    store: OfflineStore,
    notifications: SaveNotifications,
    id: Int,
    video: SavedVideo,
    parent: Uri,
    track: Pair<Int, Int>?,
): Uri? {
    val resolver = context.contentResolver
    var document: Uri? = null
    var streams = emptyList<CachedStream>()
    try {
        if (track == null) notifications.progress(id, video.title, SaveToDeviceStrings.saving(video.title), 0f)
        val container = Container.of(video.streams)
        val key = store.key
        streams = video.streams.map { openStream(it, key) }
        val audioOnly = video.streams.all { it.mimeType.startsWith("audio/") }
        val name = fileName(if (audioOnly && video.artist != null) "${video.artist} - ${video.title}" else video.title)
        val created =
            DocumentsContract.createDocument(resolver, parent, container.mimeType(audioOnly), "$name.${container.extension(audioOnly)}")
                ?: throw FileNotFoundException("The save folder refused a new file")
        document = created
        val onProgress: (Float) -> Unit = { fraction ->
            if (track == null) notifications.progress(id, video.title, SaveToDeviceStrings.saving(video.title), fraction)
        }
        val output = resolver.openOutputStream(created, "w") ?: throw FileNotFoundException("No output stream")
        output.buffered(COPY_BUFFER).use { stream ->
            when (container) {
                Container.MP4 -> {
                    val tags = Tags(video.title, video.artist, video.album, track, cover(video.cover))
                    writeMp4(streams, stream, tags, audioOnly, onProgress)
                }
                Container.WEBM -> writeWebm(streams, stream, onProgress)
            }
        }
        return created
    } catch (exception: Exception) {
        Logger.error(exception) { "Could not save ${video.videoId}" }
        document?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } }
        notifications.failed(id, video.title, reason(exception))
        return null
    } finally {
        streams.forEach(CachedStream::close)
    }
}

private fun reason(exception: Exception): String? =
    when {
        exception is StreamCacheException -> SaveToDeviceStrings.saveFailedIncomplete
        exception is UnsupportedFormatException -> SaveToDeviceStrings.saveFailedUnsupported
        exception is SecurityException || exception is FileNotFoundException -> SaveToDeviceStrings.saveFailedFolder
        generateSequence<Throwable>(exception) { it.cause }.any { (it as? ErrnoException)?.errno == OsConstants.ENOSPC } ->
            SaveToDeviceStrings.saveFailedSpace
        else -> null
    }

// Cover art as JPEG or PNG, the two image types MP4 metadata carries.
private fun cover(file: File?): Cover? {
    val bytes = file?.readBytes() ?: return null
    return when {
        bytes.startsWith(0xFF, 0xD8) -> Cover(bytes, png = false)
        bytes.startsWith(0x89, 0x50, 0x4E, 0x47) -> Cover(bytes, png = true)
        else -> {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            bitmap.recycle()
            Cover(jpeg.toByteArray(), png = false)
        }
    }
}

private fun ByteArray.startsWith(vararg prefix: Int): Boolean =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it].toByte() }

private fun fileName(title: String): String =
    title.replace(UNSAFE_NAME, "_").trim().trimEnd('.').take(MAX_NAME).ifEmpty { "_" }

private fun Uri.root(): Uri = DocumentsContract.buildDocumentUriUsingTree(this, DocumentsContract.getTreeDocumentId(this))

// The folder named [name] in [parent], created when missing, so saving a playlist again fills the same folder.
private fun ContentResolver.childFolder(tree: Uri, parent: Uri, name: String): Uri {
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent))
    val projection =
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
    query(children, projection, null, null, null)?.use { rows ->
        while (rows.moveToNext()) {
            if (rows.getString(1) == name && rows.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR)
                return DocumentsContract.buildDocumentUriUsingTree(tree, rows.getString(0))
        }
    }
    return DocumentsContract.createDocument(this, parent, DocumentsContract.Document.MIME_TYPE_DIR, name)
        ?: throw IOException("Could not create $name")
}
