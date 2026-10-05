// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.patch.ExtJsModule
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.wrapWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val hideLinkCards = patch("Hide link cards") {
    description("Hide message preview cards while keeping text links, attachments and emoji or sticker images.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Layout", DiscordSettings.hideLinkCards))

    execute {
        createMessageContent.wrapWhen(DiscordSettings.hideLinkCards, HideLinkCards.createMessageContent)
    }
}

private object HideLinkCards : ExtJsModule("discord-link-cards") {
    val createMessageContent = export("createMessageContent")
}
