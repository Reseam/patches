// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("RefreshRate")

package app.reseam.tiktok.playback

import android.view.Window
import android.view.WindowManager
import app.reseam.runtime.Export
import app.reseam.tiktok.settings.PlaybackSettings

/** TikTok's refresh rate change, with the rate left to the system while the setting is on. */
@Export
fun setAttributes(window: Window, attributes: WindowManager.LayoutParams) {
    if (PlaybackSettings.keepRefreshRate.value) attributes.preferredRefreshRate = 0f
    window.attributes = attributes
}
