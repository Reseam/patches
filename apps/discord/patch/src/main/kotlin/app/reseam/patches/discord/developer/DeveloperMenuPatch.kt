// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.developer

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val developerMenu = patch("Developer menu") {
    description("Show Discord's existing developer tools in Settings.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Developer", DiscordSettings.developerMenu))

    execute {
        gate(DiscordSettings.developerMenu) {
            createPressable.wrap(DeveloperMenu.createPressable)
        }
    }
}

private object DeveloperMenu : ExtJsModule("discord-developer-menu") {
    val createPressable by export()
}

private val createPressable = function {
    name("createPressable")
    strings("PRESSABLE")
    paramCount(1)
}
