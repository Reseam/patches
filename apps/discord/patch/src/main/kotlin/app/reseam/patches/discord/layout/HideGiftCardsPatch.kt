// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.setArgumentWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val hideGiftCards = patch("Hide gift cards") {
    description("Hide gift redemption cards in messages.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Layout", DiscordSettings.hideGiftCards))

    execute {
        createMessageContent.setArgumentWhen(DiscordSettings.hideGiftCards, 0, "options.renderGiftCode", false)
    }
}

