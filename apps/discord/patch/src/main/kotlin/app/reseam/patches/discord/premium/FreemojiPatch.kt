// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.premium

import app.reseam.discord.bindings.Freemoji
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordModules
import app.reseam.patches.discord.core.discordSettings

val freemoji = patch(DiscordPatchStrings.freemoji) {
    description(DiscordPatchStrings.freemojiDescription)
    compatibleWith(DISCORD)
    dependsOn(discordModules)
    settings(discordSettings, section(DiscordStrings.sectionNitro, DiscordSettings.freemoji))

    execute {
        gate(DiscordSettings.freemoji) {
            canUseEmojisEverywhere.before { whenFalse(Freemoji.isCheckingRealNitro()) { returnTrue() } }
            canUseAnimatedEmojis.before { whenFalse(Freemoji.isCheckingRealNitro()) { returnTrue() } }
            getEmojiUnavailableReason.before { Freemoji.checkRealNitro() }
            getEmojiUnavailableReason.after {
                returnValue(Freemoji.unlessNitroLocked(result, param(0)))
            }
            parseMessage.before { Freemoji.beginParse() }
            parseMessage.after { returnValue(Freemoji.withEmojiLinks(result)) }
        }
    }
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
