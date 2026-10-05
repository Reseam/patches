// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.setArgumentWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val staticChatGifs = patch("Static chat GIFs") {
    description("Stop GIFs from playing automatically in chat.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Media", DiscordSettings.staticChatGifs))

    execute {
        createMessageContent.setArgumentWhen(DiscordSettings.staticChatGifs, 0, "options.gifAutoPlay", false)
    }
}

