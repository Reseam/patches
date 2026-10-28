// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val disableCallIdle = patch("Disable call idle") {
    description("Stop getting kicked from empty DM calls after 3 minutes.")
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionMedia, DiscordSettings.disableCallIdle))

    execute {
        gate(DiscordSettings.disableCallIdle) {
            idleCallDisconnect.alwaysReturn()
        }
    }
}

private val idleCallDisconnect = function {
    name("disconnect")
    strings("countVoiceStatesForChannel", "sendBotMessage", "selectVoiceChannel")
    paramCount(0)
}
