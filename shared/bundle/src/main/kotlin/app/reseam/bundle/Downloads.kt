// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.bundle

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Writes a file the Downloads app lists, without a storage permission: into the shared Downloads
 * folder from Android 10, and into the app's own Downloads folder before that.
 */
fun Context.saveToDownloads(name: String, mimeType: String, content: ByteArray) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return saveToLegacyDownloads(name, mimeType, content)
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val uri = checkNotNull(contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) {
        "Downloads refused $name"
    }
    checkNotNull(contentResolver.openOutputStream(uri)).use { it.write(content) }
}

private fun Context.saveToLegacyDownloads(name: String, mimeType: String, content: ByteArray) {
    val folder = checkNotNull(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)) { "No shared storage for $name" }
    val file = File(folder, name).apply { writeBytes(content) }
    getSystemService(DownloadManager::class.java)
        .addCompletedDownload(name, name, true, mimeType, file.path, file.length(), true)
}
