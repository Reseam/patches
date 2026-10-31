// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("DownloadPermission")

package app.reseam.tiktok.download

import app.reseam.runtime.Export
import com.ss.android.ugc.aweme.feed.model.ACLCommonShare

// showType 0 hides Save and 1 hides the download action; 2 shows both.
private const val SHOWN = 2
private const val UNTRANSCODED = 1

/** Turns the author's download setting into an allowed, visible download that keeps the sound. */
@Export
fun allow(permission: ACLCommonShare?): ACLCommonShare? {
    permission?.apply {
        code = 0
        if (showType < SHOWN) showType = SHOWN
        mute = false
    }
    return permission
}

/** Asks for the original, unwatermarked file: photo downloads then pick the clean image. */
@Export
fun untranscoded(permission: ACLCommonShare?): ACLCommonShare? {
    permission?.transcode = UNTRANSCODED
    return permission
}
