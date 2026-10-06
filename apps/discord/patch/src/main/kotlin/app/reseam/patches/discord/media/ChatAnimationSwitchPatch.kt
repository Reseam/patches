// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.createMessageContent
import app.reseam.patches.discord.core.discordSettings

val chatAnimationSwitch = patch("Chat animation switch") {
    description("Use the GIF autoplay switch to also turn off chat emoji animation.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Media", DiscordSettings.chatAnimationSwitch))

    execute {
        gate(DiscordSettings.chatAnimationSwitch) {
            gifAutoplayTitle.wrap(ChatAnimationSwitch.useTitle)
            createMessageContent.wrap(ChatAnimationSwitch.createMessageContent)
        }
    }
}

private object ChatAnimationSwitch : ExtJsModule("discord-animation-switch") {
    val useTitle by export()
    val createMessageContent by export()
}

private val gifAutoplayTitle = function {
    name("useTitle")
    strings("9ptHSs", "intl")
    paramCount(0)
}
