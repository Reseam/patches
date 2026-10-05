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

val hideActivityInvites = patch("Hide activity invites") {
    description("Hide activity invite cards in messages.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Layout", DiscordSettings.hideActivityInvites))

    execute {
        createMessageContent.setArgumentWhen(DiscordSettings.hideActivityInvites, 0, "options.renderActivityInstanceEmbed", false)
        createMessageContent.setArgumentWhen(DiscordSettings.hideActivityInvites, 0, "options.renderActivityInviteEmbed", false)
    }
}

