// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val skipMaskedLinkWarnings = patch("Skip masked link warnings") {
    description("Open masked links without the destination warning.")
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.skipMaskedLinkWarnings))

    execute {
        gate(DiscordSettings.skipMaskedLinkWarnings) {
            isLinkTrusted.alwaysReturn(true)
        }
    }
}

private val isLinkTrusted = function {
    name("isLinkTrusted")
    strings("getRecipientId", "isFriend", "DM")
    paramCount(2)
}
