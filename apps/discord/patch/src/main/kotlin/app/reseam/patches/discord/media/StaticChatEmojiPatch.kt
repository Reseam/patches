// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val staticChatEmoji = patch("Static chat emoji") {
    description("Stop custom emoji from animating in chat.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Media", DiscordSettings.staticChatEmoji))

    execute {
        gate(DiscordSettings.staticChatEmoji) {
            createMessageContent.setArgument(0, "options.animateEmoji", false)
        }
    }
}

