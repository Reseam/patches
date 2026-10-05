// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.setArgumentWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val hideReactions = patch("Hide reactions") {
    description("Hide reaction chips below messages.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Layout", DiscordSettings.hideReactions))

    execute {
        createMessageContent.setArgumentWhen(DiscordSettings.hideReactions, 0, "options.renderReactions", false)
    }
}

