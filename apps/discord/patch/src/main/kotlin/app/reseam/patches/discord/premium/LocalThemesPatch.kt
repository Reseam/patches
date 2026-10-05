// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.premium

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patch.settings.returnTrueWhen
import app.reseam.patch.settings.section
import app.reseam.patch.settings.setArgumentWhen
import app.reseam.patch.settings.wrapWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val localThemes = patch("Local themes") {
    description("Use bundled Nitro themes and save your choice on this phone.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Nitro", DiscordSettings.localThemes))

    execute {
        canUseClientThemes.returnTrueWhen(DiscordSettings.localThemes)
        isPreview.returnFalseWhen(DiscordSettings.localThemes)
        initialize.setArgumentWhen(DiscordSettings.localThemes, 0, "canUseClientThemes", true)
        themePicker.wrapWhen(DiscordSettings.localThemes, LocalThemes.ThemePicker)
        shouldSync.wrapWhen(DiscordSettings.localThemes, LocalThemes.shouldSync)
    }
}

private object LocalThemes : ExtJsModule("discord-local-themes") {
    val ThemePicker = export("ThemePicker")
    val shouldSync = export("shouldSync")
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
