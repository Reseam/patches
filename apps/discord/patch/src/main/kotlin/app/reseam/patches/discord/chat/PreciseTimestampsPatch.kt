// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.patch.ExtJsModule
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.wrapWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val preciseTimestamps = patch("Precise timestamps") {
    description("Show seconds in chat timestamps.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Chat", DiscordSettings.preciseTimestamps))

    execute {
        createMessageContent.wrapWhen(DiscordSettings.preciseTimestamps, PreciseTimestamps.createMessageContent)
    }
}

private object PreciseTimestamps : ExtJsModule("discord-timestamps") {
    val createMessageContent = export("createMessageContent")
}
