// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.premium

import app.reseam.discord.bindings.AppIcons
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val premiumAppIcons = patch(DiscordPatchStrings.premiumAppIcons) {
    description(DiscordPatchStrings.premiumAppIconsDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionNitro, DiscordSettings.premiumAppIcons))

    execute {
        gate(DiscordSettings.premiumAppIcons) {
            getOfficialAlternateIcons.after { returnValue(AppIcons.allFree(result)) }
            getLimitedAlternateIcons.after { returnValue(AppIcons.allFree(result)) }
            getIconById.after { returnValue(AppIcons.asFree(result)) }
        }
    }
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
