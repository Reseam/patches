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

val hideNitroButtons = patch("Hide Nitro buttons") {
    description("Remove small Nitro promotion buttons from feature screens.")
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionLayout, DiscordSettings.hideNitroButtons))

    execute {
        gate(DiscordSettings.hideNitroButtons) {
            premiumFeatureUpsellPill.alwaysReturnNull()
        }
    }
}

private val premiumFeatureUpsellPill = function {
    name("PremiumFeatureUpsellPill")
    strings("native.PremiumFeatureUpsell", "featureName", "ShinyButton")
    paramCount(1)
}
