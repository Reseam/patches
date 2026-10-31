// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.developer

import app.reseam.discord.bindings.DeveloperMenu
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val developerMenu = patch("Developer menu") {
    description("Show Discord's existing developer tools in Settings.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section(DiscordStrings.sectionDeveloper, DiscordSettings.developerMenu))

    execute {
        gate(DiscordSettings.developerMenu) {
            createPressable.before { param(0).assign(DeveloperMenu.withDevTools(param(0))) }
        }
    }
}

private val createPressable = function {
    name("createPressable")
    strings("PRESSABLE")
    paramCount(1)
}
