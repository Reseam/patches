// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val hideGiftButton = patch(DiscordPatchStrings.hideGiftButton) {
    description(DiscordPatchStrings.hideGiftButtonDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionLayout, DiscordSettings.hideGiftButton))

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
