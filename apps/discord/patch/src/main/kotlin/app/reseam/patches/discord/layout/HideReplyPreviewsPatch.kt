// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val hideReplyPreviews = patch("Hide reply previews") {
    description("Hide the quoted message above replies.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Layout", DiscordSettings.hideReplyPreviews))

    execute {
        gate(DiscordSettings.hideReplyPreviews) {
            createMessageContent.setArgument(0, "options.renderReplies", false)
        }
    }
}

