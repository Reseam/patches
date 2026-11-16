// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("VideoCodecs")

package app.reseam.youtube.core

import app.reseam.runtime.Export
import app.reseam.youtubecommon.settings.YouTubeCommonSettings

enum class VideoCodec(val mimeType: String) {
    AV1("video/av01"),
    VP9("video/x-vnd.on2.vp9");

    val enabled: Boolean
        get() =
            !YouTubeCommonSettings.forceAvcCodec.value &&
                (this != AV1 || !YouTubeCommonSettings.forceVp9Codec.value)
}

@Export
fun codecSupported(mimeType: String?, supported: Boolean): Boolean =
    supported && VideoCodec.entries.none { it.mimeType == mimeType && !it.enabled }
