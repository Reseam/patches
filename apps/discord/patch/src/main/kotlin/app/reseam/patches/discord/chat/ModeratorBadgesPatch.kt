// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.bindings.ModeratorBadges
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordModules
import app.reseam.patches.discord.core.discordSettings

val moderatorBadges = patch("Moderator badges") {
    description("Tag the server owner, admins, managers and moderators next to their names in chat.")
    compatibleWith(DISCORD)
    dependsOn(discordModules)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.moderatorBadges))

    execute {
        gate(DiscordSettings.moderatorBadges) {
            getTagProperties.after {
                returnValue(ModeratorBadges.withStaffTag(result, param(0)))
            }
        }
    }
}

/** The tag shown after an author's name in chat, such as the bot tag. */
private val getTagProperties = function {
    name("getTagProperties")
    strings("isVerifiedBot", "BOT_TAG_SERVER_TYPE", "opTagBackgroundColor")
    paramCount(1)
}
