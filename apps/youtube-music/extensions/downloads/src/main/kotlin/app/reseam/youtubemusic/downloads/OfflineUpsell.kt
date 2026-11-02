// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("OfflineUpsell")

package app.reseam.youtubemusic.downloads

import android.net.Uri
import android.util.Base64
import app.reseam.runtime.Export
import app.reseam.youtube.core.Proto

private const val PARAMS_VIDEO_ID = 4
private const val PARAMS_COLLECTION = 5
private const val COLLECTION_PLAYLIST = 10
private const val PLAYLIST_ID = 2
private const val ALBUM_PLAYLIST_ID = 3

/** The video a song's offline upsell is for, null for a playlist; params are a URL-encoded base64url protobuf. */
@Export
fun videoId(params: String): String? = Proto.string(decode(params), PARAMS_VIDEO_ID)

/** The playlist or album an offline upsell is for, null for a song. */
@Export
fun playlistId(params: String): String? {
    val collection = Proto.bytes(decode(params), PARAMS_COLLECTION) ?: return null
    val playlist = Proto.bytes(collection, COLLECTION_PLAYLIST) ?: return null
    return Proto.string(playlist, PLAYLIST_ID) ?: Proto.string(playlist, ALBUM_PLAYLIST_ID)
}

private fun decode(params: String) = Base64.decode(Uri.decode(params), Base64.URL_SAFE)
