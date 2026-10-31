// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val hideGiftCards = patch("Hide gift cards") {
    description("Hide gift redemption cards in messages.")
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionLayout, DiscordSettings.hideGiftCards))

    execute {
        gate(DiscordSettings.hideGiftCards) {
            createMessageContent.setArgument(0, "options.renderGiftCode", false)
        }
    }
}

