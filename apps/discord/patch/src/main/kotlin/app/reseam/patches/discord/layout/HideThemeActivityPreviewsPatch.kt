// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.returnNullWhen
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val hideThemeActivityPreviews = patch("Hide theme activity previews") {
    description("Remove activity cards from the Appearance preview.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Layout", DiscordSettings.hideThemeActivityPreviews))

    execute {
        activityCardsItem.returnNullWhen(DiscordSettings.hideThemeActivityPreviews)
    }
}

private val activityCardsItem = function {
    name("ActivityCardsItem")
    strings("FlashList", "cards", "animatedStyles")
    paramCount(1)
}
