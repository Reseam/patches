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

/** Writes a file to the shared Downloads folder without a storage permission. */
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

// Before Android 10 only the app's own folder is writable without a permission. Registering the
// file lists it in the Downloads app.
private fun Context.saveToLegacyDownloads(name: String, mimeType: String, content: ByteArray) {
    val folder = checkNotNull(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)) { "No shared storage for $name" }
    val file = File(folder, name).apply { writeBytes(content) }
    getSystemService(DownloadManager::class.java)
        .addCompletedDownload(name, name, true, mimeType, file.path, file.length(), true)
}
