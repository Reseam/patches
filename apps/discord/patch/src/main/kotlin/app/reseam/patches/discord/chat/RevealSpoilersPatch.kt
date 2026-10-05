// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patch.settings.returnTrueWhen
import app.reseam.patch.settings.section
import app.reseam.patch.settings.setArgumentWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val revealSpoilers = patch("Reveal spoilers") {
    description("Show spoiler text and media without tapping to reveal them.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Chat", DiscordSettings.revealSpoilers))

    execute {
        computeGlobalSpoilerDisplay.returnTrueWhen(DiscordSettings.revealSpoilers)
        useShouldDisplaySpoilerObscurity.returnFalseWhen(DiscordSettings.revealSpoilers)
        createMessageContent.setArgumentWhen(DiscordSettings.revealSpoilers, 0, "options.shouldObscureSpoiler", false)
    }
}

private val computeGlobalSpoilerDisplay = function {
    name("computeGlobalSpoilerDisplay")
    strings("ALWAYS", "IF_MODERATOR", "ON_CLICK")
    paramCount(2)
}

private val useShouldDisplaySpoilerObscurity = function {
    name("useShouldDisplaySpoilerObscurity")
    strings("RenderSpoilers", "ON_CLICK", "useSetting")
    paramCount(1)
}
