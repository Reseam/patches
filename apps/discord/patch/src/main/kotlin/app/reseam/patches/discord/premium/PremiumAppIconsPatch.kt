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

val premiumAppIcons = patch("Premium app icons") {
    description("Use the premium launcher icons included in Discord.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Nitro", DiscordSettings.premiumAppIcons))

    execute {
        getOfficialAlternateIcons.wrapWhen(DiscordSettings.premiumAppIcons, PremiumAppIcons.getOfficialAlternateIcons)
        getLimitedAlternateIcons.wrapWhen(DiscordSettings.premiumAppIcons, PremiumAppIcons.getLimitedAlternateIcons)
        getIconById.wrapWhen(DiscordSettings.premiumAppIcons, PremiumAppIcons.getIconById)
    }
}

private object PremiumAppIcons : ExtJsModule("discord-app-icons") {
    val getOfficialAlternateIcons = export("getOfficialAlternateIcons")
    val getLimitedAlternateIcons = export("getLimitedAlternateIcons")
    val getIconById = export("getIconById")
}

private val getOfficialAlternateIcons = function {
    name("getOfficialAlternateIcons")
    strings("filter")
    paramCount(0)
}

private val getLimitedAlternateIcons = function {
    name("getLimitedAlternateIcons")
    strings("filter")
    paramCount(0)
}

private val getIconById = function {
    name("getIconById")
    strings("find")
    paramCount(1)
}
