// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("DisableAutoCaptions")

package app.reseam.youtube.playerui

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.PlayerOverlaySettings

@Volatile private var captionsButtonStatus = false

@Export
fun disableAutoCaptions(): Boolean {
    // A chosen captions language turns captions on by design.
    val disable =
        PlayerOverlaySettings.disableAutoCaptions.value &&
            !captionsButtonStatus &&
            PlayerOverlaySettings.captionLanguage.value == CAPTION_LANGUAGE_OFF
    Logger.debug { "DisableAutoCaptions: $disable status=$captionsButtonStatus" }
    return disable
}

@Export
fun setCaptionsButtonStatus(status: Boolean) {
    captionsButtonStatus = status
    Logger.debug { "DisableAutoCaptions.status: $status" }
}
