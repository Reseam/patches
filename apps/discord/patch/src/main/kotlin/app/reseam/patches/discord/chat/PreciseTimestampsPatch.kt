// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.bindings.Timestamps
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val preciseTimestamps = patch(DiscordPatchStrings.preciseTimestamps) {
    description(DiscordPatchStrings.preciseTimestampsDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.preciseTimestamps))

    execute {
        gate(DiscordSettings.preciseTimestamps) {
            createMessageContent.after {
                returnValue(Timestamps.withSeconds(result, param(0)))
            }
        }
    }
}
