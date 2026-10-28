// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.bindings.BlockedMessages
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordModules
import app.reseam.patches.discord.core.discordSettings

val hideBlockedMessages = patch("Hide blocked messages") {
    description("Remove messages from blocked and ignored users from chat entirely.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    dependsOn(discordModules)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.hideBlockedMessages))

    execute {
        gate(DiscordSettings.hideBlockedMessages) {
            createRow.before {
                whenTrue(BlockedMessages.isBlockedGroup(param(0))) { returnUndefined() }
            }
        }
    }
}

private val createRow = function {
    name("createRow")
    strings("rowIndex", "MESSAGE")
    paramCount(1)
}
