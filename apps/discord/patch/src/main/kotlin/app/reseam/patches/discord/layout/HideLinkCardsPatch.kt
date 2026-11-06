// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.discord.bindings.LinkCards
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val hideLinkCards = patch("Hide link cards") {
    description("Hide message preview cards while keeping text links, attachments and emoji or sticker images.")
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionLayout, DiscordSettings.hideLinkCards))

    execute {
        gate(DiscordSettings.hideLinkCards) {
            createMessageContent.before { param(0).assign(LinkCards.withoutLinkCards(param(0))) }
        }
    }
}
