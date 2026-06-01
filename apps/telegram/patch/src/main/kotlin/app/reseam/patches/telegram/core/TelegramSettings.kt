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

    val SaveFromRestricted = ToggleSetting(
        key = "privacy.save_from_restricted",
        title = "Save from restricted chats",
        summary = "Re-enables copy, save, and forward in chats with content protection on.",
        default = true,
    )

    val RecoverDeleted = ToggleSetting(
        key = "privacy.recover_deleted",
        title = "Recover deleted messages",
        summary = "Keep messages others delete; they stay in the chat with a 🗑️ marker.",
        default = true,
    )

    val AllowScreenshots = ToggleSetting(
        key = "privacy.allow_screenshots",
        title = "Allow screenshots in secret viewers",
        summary = "Disable FLAG_SECURE so you can screenshot or record view-once / self-destruct media.",
        default = true,
    )
}
