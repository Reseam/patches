// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("LiveAvatar")

package app.reseam.youtube.misc

import android.net.Uri
import android.util.Base64
import app.reseam.runtime.Export
import app.reseam.youtube.core.Endpoints
import app.reseam.youtube.core.InnerTube
import app.reseam.youtube.core.Proto
import app.reseam.youtube.navigation.Commands

// Taps on a channel's live ring carry this in their watch params; other taps on the same video do not.
private const val PARAMS_SOURCE = 71
private const val SOURCE_LIVE_RING = 48L

/** Returns true when the tap was a live ring, which then opens the channel instead of the stream. */
@Export
fun openChannel(endpoint: ByteArray): Boolean {
    val watch = Proto.bytes(endpoint, Endpoints.WATCH) ?: return false
    val params = Proto.string(watch, Endpoints.WATCH_PARAMS) ?: return false
    val source =
        try {
            Proto.number(Base64.decode(Uri.decode(params), Base64.URL_SAFE), PARAMS_SOURCE, 0)
        } catch (_: IllegalArgumentException) {
            return false
        }
    if (source != SOURCE_LIVE_RING) return false
    val videoId = Proto.string(watch, Endpoints.WATCH_VIDEO_ID) ?: return false
    // Without the channel, the tap does what YouTube would have done.
    InnerTube.channelId(videoId) { channelId ->
        if (channelId == null || !Commands.openChannel(channelId)) Commands.openVideo(videoId)
    }
    return true
}
