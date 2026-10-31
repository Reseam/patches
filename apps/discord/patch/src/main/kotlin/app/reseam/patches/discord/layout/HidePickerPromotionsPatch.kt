// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.discord.bindings.PickerPromotions
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val hidePickerPromotions = patch("Hide picker promotions") {
    description("Remove Nitro promotions from emoji and sticker search.")
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionLayout, DiscordSettings.hidePickerPromotions))

    execute {
        gate(DiscordSettings.hidePickerPromotions) {
            premiumExpressionPickerSearchUpsell.alwaysReturnNull()
            premiumFeatureUpsell.before {
                whenTrue(PickerPromotions.isPickerUpsell(param(0))) { returnNull() }
            }
        }
    }
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
