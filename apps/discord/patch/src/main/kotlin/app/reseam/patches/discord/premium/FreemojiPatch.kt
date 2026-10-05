// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.premium

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.wrapWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val freemoji = patch("Freemoji") {
    description("Send locked custom emoji as image links.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Nitro", DiscordSettings.freemoji))

    execute {
        canUseEmojisEverywhere.wrapWhen(DiscordSettings.freemoji, Freemoji.canUseEmojisEverywhere)
        canUseAnimatedEmojis.wrapWhen(DiscordSettings.freemoji, Freemoji.canUseAnimatedEmojis)
        getEmojiUnavailableReason.wrapWhen(DiscordSettings.freemoji, Freemoji.getEmojiUnavailableReason)
        parseMessage.wrapWhen(DiscordSettings.freemoji, Freemoji.parse)
    }
}

private object Freemoji : ExtJsModule("discord-freemoji") {
    val canUseEmojisEverywhere = export("canUseEmojisEverywhere")
    val canUseAnimatedEmojis = export("canUseAnimatedEmojis")
    val getEmojiUnavailableReason = export("getEmojiUnavailableReason")
    val parse = export("parse")
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
