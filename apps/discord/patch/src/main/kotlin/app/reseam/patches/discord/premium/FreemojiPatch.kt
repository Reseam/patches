// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.premium

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val freemoji = patch("Freemoji") {
    description("Send locked custom emoji as image links.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Nitro", DiscordSettings.freemoji))

    execute {
        gate(DiscordSettings.freemoji) {
            canUseEmojisEverywhere.wrap(Freemoji.canUseEmojisEverywhere)
            canUseAnimatedEmojis.wrap(Freemoji.canUseAnimatedEmojis)
            getEmojiUnavailableReason.wrap(Freemoji.getEmojiUnavailableReason)
            parseMessage.wrap(Freemoji.parse)
        }
    }
}

private object Freemoji : ExtJsModule("discord-freemoji") {
    val canUseEmojisEverywhere by export()
    val canUseAnimatedEmojis by export()
    val getEmojiUnavailableReason by export()
    val parse by export()
}

private val canUseEmojisEverywhere = function {
    name("canUseEmojisEverywhere")
    strings("EMOJIS_EVERYWHERE", "canUserUse")
    paramCount(1)
}

private val canUseAnimatedEmojis = function {
    name("canUseAnimatedEmojis")
    strings("ANIMATED_EMOJIS", "canUserUse")
    paramCount(1)
}

private val getEmojiUnavailableReason = function {
    name("getEmojiUnavailableReason")
    strings("bypassPremiumEmojiEntitlement", "canUseAnimatedEmojis", "canUseEmojisEverywhere")
    paramCount(1)
}

private val parseMessage = function {
    name("parse")
    strings("translateInlineEmojiToSurrogates", "content")
    paramCount(2)
}
