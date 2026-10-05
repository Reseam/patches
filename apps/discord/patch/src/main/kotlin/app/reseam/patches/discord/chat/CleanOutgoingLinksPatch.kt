// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.wrapWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val cleanOutgoingLinks = patch("Clean outgoing links") {
    description("Remove tracking parameters from supported links when you send them.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Chat", DiscordSettings.cleanOutgoingLinks))

    execute {
        sendMessage.wrapWhen(DiscordSettings.cleanOutgoingLinks, CleanOutgoingLinks.sendMessage)
    }
}

private object CleanOutgoingLinks : ExtJsModule("discord-clean-links") {
    val sendMessage = export("sendMessage")
}

private val sendMessage = function {
    name("_sendMessage")
    paramCount(3)
}
