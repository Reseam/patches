// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.bindings.MessageHistory
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val keepDeletedMessages = patch("Keep deleted messages") {
    description("Keep loaded messages after deletion and highlight them in red.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    dependsOn(messageHistory)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.keepDeletedMessages))

    execute {
        gate(DiscordSettings.keepDeletedMessages) {
            handleMessageDelete.before {
                whenTrue(MessageHistory.keepDeletedMessage(param(0))) { returnTrue() }
            }
            handleMessageDeleteBulk.before {
                param(0).assign(MessageHistory.withoutKeptMessages(param(0)))
            }
            handleMessageDeleteBulk.after { returnValue(MessageHistory.bulkDeleteResult(result)) }
            createBackgroundHighlight.after { returnValue(MessageHistory.deletedHighlight(param(0), result)) }
        }
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

/** Picks a chat row's background and gutter colors, such as the mention and automod highlights. */
private val createBackgroundHighlight = function {
    name("createBackgroundHighlight")
    strings("isAutomodBlockedMessage", "editingColor")
    paramCount(1)
}
