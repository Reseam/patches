// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.guild

import app.reseam.discord.bindings.MarkAllServersRead
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

val markAllServersRead = patch(DiscordPatchStrings.markAllServersRead) {
    description(DiscordPatchStrings.markAllServersReadDescription)
    compatibleWith(DISCORD)
    dependsOn(discordModules)
    settings(discordSettings, section(DiscordStrings.sectionServers, DiscordSettings.markAllServersRead))

    execute {
        gate(DiscordSettings.markAllServersRead) {
            guildsBarGuildMenuItems.after { returnValue(MarkAllServersRead.withMarkAllItem(result)) }
            guildUnreadAction.after { returnValue(MarkAllServersRead.withMarkAllRow(result)) }
        }
    }
}

/** The menu a long press on a server in the server list opens. */
private val guildsBarGuildMenuItems = function {
    name("getGuildsBarGuildMenuItems")
    strings("shouldShowUseNewNotificationSystem")
    paramCount(1)
}

/** The first group of a server's options sheet: Mark as read, mute and browse channels. */
private val guildUnreadAction = function {
    name("GuildUnreadAction")
    strings("useShouldUseNewNotificationSystem", "ActionSheetRow")
    paramCount(1)
}
