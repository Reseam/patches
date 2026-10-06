// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val showBlockedMessages = patch("Show blocked messages") {
    description("Expand received messages from blocked users without unblocking them.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Chat", DiscordSettings.showBlockedMessages))

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
