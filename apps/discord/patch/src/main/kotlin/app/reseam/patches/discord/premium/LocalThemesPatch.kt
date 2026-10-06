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

val localThemes = patch("Local themes") {
    description("Use bundled Nitro themes and save your choice on this phone.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Nitro", DiscordSettings.localThemes))

    execute {
        gate(DiscordSettings.localThemes) {
            canUseClientThemes.alwaysReturn(true)
            isPreview.alwaysReturn(false)
            initialize.setArgument(0, "canUseClientThemes", true)
            themePicker.wrap(LocalThemes.ThemePicker)
            shouldSync.wrap(LocalThemes.shouldSync)
        }
    }
}

private object LocalThemes : ExtJsModule("discord-local-themes") {
    val ThemePicker by export()
    val shouldSync by export()
}

private val canUseClientThemes = function {
    name("canUseClientThemes")
    strings("CLIENT_THEMES", "canUserUse")
    paramCount(1)
}

private val initialize = function {
    name("initialize")
    strings("canUseClientThemes", "gradientPresetId", "syncWith")
    paramCount(1)
}

// A getter that references no strings, so its name and parameter count identify it.
private val isPreview = function {
    name("isPreview")
    paramCount(0)
}

private val themePicker = function {
    name("ThemePicker")
    strings("CLIENT_THEMES_THEME_SELECTOR", "themeSelector", "isPreview")
    paramCount(1)
}

private val shouldSync = function {
    name("shouldSync")
    strings("shouldSync")
    paramCount(1)
}
