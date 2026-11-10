// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.bindings.QuickReplyAndDelete
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordModules
import app.reseam.patches.discord.core.discordSettings

val quickReplyAndDelete = patch(DiscordPatchStrings.quickReplyAndDelete) {
    description(DiscordPatchStrings.quickReplyAndDeleteDescription)
    compatibleWith(DISCORD)
    dependsOn(discordModules)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.quickReplyAndDelete))

    execute {
        gate(DiscordSettings.quickReplyAndDelete) {
            createMessagesHandlers.after { returnValue(QuickReplyAndDelete.withQuickActions(result)) }
        }
    }
}

/** Creates the handlers a chat passes to its native message list, once per chat screen. */
private val createMessagesHandlers = function {
    name("")
    strings("MessagesHandlers")
    paramCount(0)
}
