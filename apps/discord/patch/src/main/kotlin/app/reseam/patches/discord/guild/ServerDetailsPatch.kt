// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.guild

import app.reseam.discord.bindings.ServerDetails
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

val serverDetails = patch(DiscordPatchStrings.serverDetails) {
    description(DiscordPatchStrings.serverDetailsDescription)
    compatibleWith(DISCORD)
    dependsOn(discordModules)
    settings(discordSettings, section(DiscordStrings.sectionServers, DiscordSettings.serverDetails))

    execute {
        gate(DiscordSettings.serverDetails) {
            guildActionSheetSecondaryActions.after {
                returnValue(ServerDetails.withDetails(result, param(0)))
            }
        }
    }
}

/** The lower group of a server's options sheet, such as notifications and privacy. */
private val guildActionSheetSecondaryActions = function {
    name("GuildActionSheetSecondaryActions")
    strings("useCanReportRaid", "useGuildIncidentsState")
    paramCount(1)
}
