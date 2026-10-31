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

val localEditHistory = patch("Local edit history") {
    description("Show up to three previously displayed text versions of edited messages.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    dependsOn(messageHistory)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.localEditHistory))

    execute {
        gate(DiscordSettings.localEditHistory) {
            updateMessageRecord.after {
                MessageHistory.recordEdit(param(0), param(1), result)
            }
        }
    }
}

private val updateMessageRecord = function {
    name("updateMessageRecord")
    strings("edited_timestamp", "mergeEmbedsOnURL", "set")
    paramCount(2)
}
