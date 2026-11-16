// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("VideoCodecs")

package app.reseam.youtube.misc

import app.reseam.runtime.Export
import app.reseam.youtubecommon.settings.YouTubeCommonSettings

@Export
fun codecSupported(mimeType: String?, supported: Boolean): Boolean =
    supported && !(mimeType == "video/av01" && YouTubeCommonSettings.forceVp9Codec.value)
