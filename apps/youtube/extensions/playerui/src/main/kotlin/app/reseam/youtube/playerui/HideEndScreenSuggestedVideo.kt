// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("HideEndScreenSuggestedVideo")

package app.reseam.youtube.playerui

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.PlayerOverlaySettings

@Volatile private var autoplayEnabled = false

@Export
fun setAutoplayStatus(enabled: Boolean) {
    autoplayEnabled = enabled
    Logger.debug { "HideEndScreenSuggestedVideo.autoplay: $enabled" }
}

@Export
fun shouldHide(): Boolean {
    val hide = PlayerOverlaySettings.hideEndScreenSuggestedVideo.value && !autoplayEnabled
    Logger.debug { "HideEndScreenSuggestedVideo: $hide autoplay=$autoplayEnabled" }
    return hide
}
