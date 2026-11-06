// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val staticChatGifs = patch("Static chat GIFs") {
    description("Stop GIFs from playing automatically in chat.")
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionMedia, DiscordSettings.staticChatGifs))

    execute {
        gate(DiscordSettings.staticChatGifs) {
            createMessageContent.setArgument(0, "options.gifAutoPlay", false)
        }
    }
}

