// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("VideoCodecs")

package app.reseam.youtube.misc

import android.view.Display
import app.reseam.runtime.Export
import app.reseam.youtube.settings.VideoSettings

@Export
fun supportedHdrTypes(capabilities: Display.HdrCapabilities): IntArray =
    if (VideoSettings.disableHdrVideo.value) intArrayOf() else capabilities.supportedHdrTypes
