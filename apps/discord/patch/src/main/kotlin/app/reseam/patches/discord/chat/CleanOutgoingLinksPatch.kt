// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.bindings.CleanLinks
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val cleanOutgoingLinks = patch(DiscordPatchStrings.cleanOutgoingLinks) {
    description(DiscordPatchStrings.cleanOutgoingLinksDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.cleanOutgoingLinks))

    execute {
        gate(DiscordSettings.cleanOutgoingLinks) {
            sendMessage.before { param(1).assign(CleanLinks.cleanLinks(param(1))) }
        }
    }
}

private val sendMessage = function {
    name("_sendMessage")
    paramCount(3)
}
