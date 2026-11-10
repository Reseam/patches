// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val hideActivityInvites = patch(DiscordPatchStrings.hideActivityInvites) {
    description(DiscordPatchStrings.hideActivityInvitesDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionLayout, DiscordSettings.hideActivityInvites))

    execute {
        gate(DiscordSettings.hideActivityInvites) {
            createMessageContent.setArgument(0, "options.renderActivityInstanceEmbed", false)
            createMessageContent.setArgument(0, "options.renderActivityInviteEmbed", false)
        }
    }
}

