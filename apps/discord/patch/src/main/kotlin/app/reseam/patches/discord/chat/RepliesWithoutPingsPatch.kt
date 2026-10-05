// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.setArgumentWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val repliesWithoutPings = patch("Replies without pings") {
    description("Start replies with mentions turned off. You can still turn them on.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Chat", DiscordSettings.repliesWithoutPings))

    execute {
        createPendingReply.setArgumentWhen(DiscordSettings.repliesWithoutPings, 0, "shouldMention", false)
        createPendingReply.setArgumentWhen(DiscordSettings.repliesWithoutPings, 0, "showMentionToggle", true)
    }
}

private val createPendingReply = function {
    name("createPendingReply")
    strings("shouldMention", "showMentionToggle", "dispatch")
    paramCount(1)
}
