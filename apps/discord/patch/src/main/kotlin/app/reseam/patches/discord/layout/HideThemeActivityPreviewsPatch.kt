// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val hideThemeActivityPreviews = patch("Hide theme activity previews") {
    description("Remove activity cards from the Appearance preview.")
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionLayout, DiscordSettings.hideThemeActivityPreviews))

    execute {
        gate(DiscordSettings.hideThemeActivityPreviews) {
            activityCardsItem.alwaysReturnNull()
        }
    }
}

private val activityCardsItem = function {
    name("ActivityCardsItem")
    strings("FlashList", "cards", "animatedStyles")
    paramCount(1)
}
