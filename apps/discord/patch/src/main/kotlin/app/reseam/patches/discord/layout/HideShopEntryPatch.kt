// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.discord.bindings.Shop
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val hideShopEntry = patch(DiscordPatchStrings.hideShopEntry) {
    description(DiscordPatchStrings.hideShopEntryDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionLayout, DiscordSettings.hideShopEntry))

    execute {
        gate(DiscordSettings.hideShopEntry) {
            shopEntryButton.alwaysReturnNull()
            nitroCard.after { returnValue(Shop.withoutShop(result)) }
            createRoute.before { param(0).assign(Shop.withoutShopRoute(param(0))) }
        }
    }
}

private val shopEntryButton = function {
    name("CollectiblesShopEntryButton")
    strings("COLLECTIBLES_SHOP_ENTRY_MARKETING", "navigateToShop")
    paramCount(1)
}

private val nitroCard = function {
    name("children")
    strings("USER_PROFILE_PREMIUM_AND_SHOP_ENTRY_POINTS", "ShopIcon", "NitroWheelIcon")
    paramCount(1)
}

private val createRoute = function {
    name("createRoute")
    strings("ROUTE")
    paramCount(1)
}
