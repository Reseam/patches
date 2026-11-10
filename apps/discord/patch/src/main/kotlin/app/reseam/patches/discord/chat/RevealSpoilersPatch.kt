// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val revealSpoilers = patch(DiscordPatchStrings.revealSpoilers) {
    description(DiscordPatchStrings.revealSpoilersDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.revealSpoilers))

    execute {
        gate(DiscordSettings.revealSpoilers) {
            computeGlobalSpoilerDisplay.alwaysReturn(true)
            useShouldDisplaySpoilerObscurity.alwaysReturn(false)
            createMessageContent.setArgument(0, "options.shouldObscureSpoiler", false)
        }
    }
}

private val computeGlobalSpoilerDisplay = function {
    name("computeGlobalSpoilerDisplay")
    strings("ALWAYS", "IF_MODERATOR", "ON_CLICK")
    paramCount(2)
}

private val useShouldDisplaySpoilerObscurity = function {
    name("useShouldDisplaySpoilerObscurity")
    strings("RenderSpoilers", "ON_CLICK", "useSetting")
    paramCount(1)
}
