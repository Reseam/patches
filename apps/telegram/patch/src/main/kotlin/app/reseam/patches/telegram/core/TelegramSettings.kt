// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.core

import app.reseam.patch.settings.toggle

object TelegramSettings {
    val hideSponsoredAds by toggle("Hide sponsored messages", default = true)
    val disableAutoUpdate by toggle("Disable auto-update", default = true)
    val unlockPremium by toggle(
        "Unlock Premium",
        summary = "Server-checked features still need a real subscription.",
        default = true,
    )
    val hideTyping by toggle("Hide typing indicator", default = true)
    val boostDownloads by toggle("Boost download speed", default = true)
    val saveFromRestricted by toggle(
        "Save from restricted chats",
        summary = "Re-enables copy, save, and forward in chats with content protection on.",
        default = true,
    )
    val recoverDeleted by toggle(
        "Recover deleted messages",
        summary = "Keep messages others delete; they stay in the chat with a 🗑️ marker.",
        default = true,
    )
    val allowScreenshots by toggle(
        "Allow screenshots in secret viewers",
        summary = "Disable FLAG_SECURE so you can screenshot or record view-once / self-destruct media.",
        default = true,
    )
}
