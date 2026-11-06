// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.discord.bindings.AnimationSwitch
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val chatAnimationSwitch = patch("Chat animation switch") {
    description("Use the GIF autoplay switch to also turn off chat emoji animation.")
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionMedia, DiscordSettings.chatAnimationSwitch))

    execute {
        gate(DiscordSettings.chatAnimationSwitch) {
            gifAutoplayTitle.alwaysReturn("Animate chat GIFs and emoji")
            createMessageContent.before {
                param(0).assign(AnimationSwitch.withoutEmojiAnimation(param(0)))
            }
        }
    }
}

private val gifAutoplayTitle = function {
    name("useTitle")
    strings("9ptHSs", "intl")
    paramCount(0)
}
