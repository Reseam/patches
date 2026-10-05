// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val hideGiftButton = patch("Hide gift button") {
    description("Remove the gift button from the message box.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Layout", DiscordSettings.hideGiftButton))

    execute {
        gate(DiscordSettings.hideGiftButton) {
            chatInputActionButtonGift.alwaysReturnNull()
        }
    }
}

private val chatInputActionButtonGift = function {
    name("ChatInputActionButtonGift")
    strings("GiftIcon", "GiftIconTrinketsAnimation")
    paramCount(1)
}
