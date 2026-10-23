// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SaveToDevice")

package app.reseam.youtube.savetodevice

import android.app.Activity
import android.content.Context
import android.net.Uri
import app.reseam.runtime.Export
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtubesavetodevice.settings.SaveToDeviceSettings

// The offline store's media status once every stream of a download is stored.
private const val COMPLETE = "COMPLETE"

/** Saves a download as it completes, when the user asked for every download to be saved. */
@Export
fun onDownloadStatus(videoId: String?, status: String?) {
    if (videoId == null || status != COMPLETE || !SaveToDeviceSettings.saveNewDownloads.value) return
    val context = YouTubeContext.applicationContext
    val tree = saveFolder(context) ?: return
    enqueue(context, tree, SaveTarget.Video(videoId))
}

/** Saves [target], asking for the save folder first when none is set. */
internal fun request(activity: Activity, target: SaveTarget) {
    val context = activity.applicationContext
    saveFolder(context)?.let { return enqueue(context, it, target) }
    SaveToDeviceSettings.saveFolder.pick(activity) { saveFolder(context)?.let { enqueue(context, it, target) } }
}

// The chosen folder while the app still holds write access to it.
private fun saveFolder(context: Context): Uri? {
    val folder = SaveToDeviceSettings.saveFolder.value.takeIf { it.startsWith("content://") }?.let(Uri::parse)
    return folder?.takeIf { uri ->
        context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
    }
}
