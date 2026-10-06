// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
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
        gate(DiscordSettings.hideReactions) {
            createMessageContent.setArgument(0, "options.renderReactions", false)
        }
    }
}

