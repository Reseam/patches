// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val showBlockedMessages = patch(DiscordPatchStrings.showBlockedMessages) {
    description(DiscordPatchStrings.showBlockedMessagesDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.showBlockedMessages))

    execute {
        gate(DiscordSettings.showBlockedMessages) {
            isBlockedForMessage.alwaysReturn(false)
        }
    }
}

private val isBlockedForMessage = function {
    name("isBlockedForMessage")
    strings("interactionMetadata", "interaction_metadata", "BLOCKED")
    paramCount(1)
}
