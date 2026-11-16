// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("DisplayHdr")
@file:Suppress("DEPRECATION")

package app.reseam.youtube.core

import android.hardware.display.DisplayManager
import android.view.Display
import app.reseam.runtime.Export
import app.reseam.youtubecommon.settings.YouTubeCommonSettings

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
