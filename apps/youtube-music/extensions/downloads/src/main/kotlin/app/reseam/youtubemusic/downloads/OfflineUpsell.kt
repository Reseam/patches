// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("OfflineUpsell")

package app.reseam.youtubemusic.downloads

import android.net.Uri
import android.util.Base64
import app.reseam.runtime.Export
import app.reseam.youtube.core.Proto

private const val PARAMS_VIDEO_ID = 4

/** The video a song's offline upsell is for, null for a playlist; params are a URL-encoded base64url protobuf. */
@Export
fun videoId(params: String): String? =
    Proto.string(Base64.decode(Uri.decode(params), Base64.URL_SAFE), PARAMS_VIDEO_ID)
