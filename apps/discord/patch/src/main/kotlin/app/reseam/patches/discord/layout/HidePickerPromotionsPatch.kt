// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val hidePickerPromotions = patch("Hide picker promotions") {
    description("Remove Nitro promotions from emoji and sticker search.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Layout", DiscordSettings.hidePickerPromotions))

    execute {
        gate(DiscordSettings.hidePickerPromotions) {
            premiumExpressionPickerSearchUpsell.alwaysReturnNull()
            premiumFeatureUpsell.wrap(PickerPromotions.PremiumFeatureUpsell)
        }
    }
}

private object PickerPromotions : ExtJsModule("discord-picker-promotions") {
    val PremiumFeatureUpsell by export()
}

private val premiumExpressionPickerSearchUpsell = function {
    name("PremiumExpressionPickerSearchUpsell")
    strings("ctaText", "PressableOpacity", "upsell")
    paramCount(1)
}

private val premiumFeatureUpsell = function {
    name("PremiumFeatureUpsell")
    strings("featureName", "useAnalyticsContext", "shouldShow")
    paramCount(1)
}
