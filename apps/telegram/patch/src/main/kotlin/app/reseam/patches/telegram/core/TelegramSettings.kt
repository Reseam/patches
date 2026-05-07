// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.core

import app.reseam.patch.settings.ToggleSetting

object TelegramSettings {
    val HideSponsoredAds = ToggleSetting(
        key = "ads.hide_sponsored",
        title = "Hide sponsored messages",
        default = true,
    )

    val DisableAutoUpdate = ToggleSetting(
        key = "update.disable_auto_check",
        title = "Disable auto-update",
        default = true,
    )

    val UnlockPremium = ToggleSetting(
        key = "premium.unlock_client",
        title = "Unlock Premium",
        summary = "Server-checked features still need a real subscription.",
        default = true,
    )

    val HideTyping = ToggleSetting(
        key = "privacy.hide_typing",
        title = "Hide typing indicator",
        default = true,
    )

    val BoostDownloads = ToggleSetting(
        key = "downloads.boost",
        title = "Boost download speed",
        default = true,
    )
}
