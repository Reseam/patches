// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.wrapWhen
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
        gifAutoplayTitle.wrapWhen(DiscordSettings.chatAnimationSwitch, ChatAnimationSwitch.useTitle)
        createMessageContent.wrapWhen(DiscordSettings.chatAnimationSwitch, ChatAnimationSwitch.createMessageContent)
    }
}

private object ChatAnimationSwitch : ExtJsModule("discord-animation-switch") {
    val useTitle = export("useTitle")
    val createMessageContent = export("createMessageContent")
}

private val gifAutoplayTitle = function {
    name("useTitle")
    strings("9ptHSs", "intl")
    paramCount(0)
}
