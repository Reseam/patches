// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.wrapWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val keepDeletedMessages = patch("Keep deleted messages") {
    description("Keep loaded messages after deletion and mark them as deleted.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    dependsOn(messageHistory)
    settings(discordSettings, section("Chat", DiscordSettings.keepDeletedMessages))

    execute {
        handleMessageDelete.wrapWhen(DiscordSettings.keepDeletedMessages, MessageHistory.handleMessageDelete)
        handleMessageDeleteBulk.wrapWhen(DiscordSettings.keepDeletedMessages, MessageHistory.handleMessageDeleteBulk)
    }
}

private val handleMessageDelete = function {
    name("handleMessageDelete")
    strings("revealedMessageId", "getOrCreate", "commit")
    paramCount(1)
}

private val handleMessageDeleteBulk = function {
    name("handleMessageDeleteBulk")
    strings("revealedMessageId", "removeMany", "commit")
    paramCount(1)
}
