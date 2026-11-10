// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val staticChatEmoji = patch(DiscordPatchStrings.staticChatEmoji) {
    description(DiscordPatchStrings.staticChatEmojiDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionMedia, DiscordSettings.staticChatEmoji))

    execute {
        gate(DiscordSettings.staticChatEmoji) {
            createMessageContent.setArgument(0, "options.animateEmoji", false)
        }
    }
}

