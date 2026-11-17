// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("VideoCapabilities")
@file:Suppress("DEPRECATION")

package app.reseam.youtube.core

import android.hardware.display.DisplayManager
import android.view.Display
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
    supported && VideoCodec.entries.firstOrNull { it.mimeType == mimeType }?.enabled != false

@Export
fun supportedHdrTypes(capabilities: Display.HdrCapabilities): IntArray =
    if (YouTubeCommonSettings.disableHdrVideo.value) intArrayOf() else capabilities.supportedHdrTypes

fun defaultDisplayHdrTypes(): IntArray =
    YouTubeContext.applicationContext
        .getSystemService(DisplayManager::class.java)
        .getDisplay(Display.DEFAULT_DISPLAY)
        ?.hdrCapabilities
        ?.let(::supportedHdrTypes)
        ?: intArrayOf()
