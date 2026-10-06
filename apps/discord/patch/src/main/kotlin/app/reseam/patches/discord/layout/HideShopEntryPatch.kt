// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val hideShopEntry = patch("Hide shop entry") {
    description("Remove Shop buttons from profiles and the Shop entry in settings.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Layout", DiscordSettings.hideShopEntry))

    execute {
        gate(DiscordSettings.hideShopEntry) {
            shopEntryButton.alwaysReturnNull()
            nitroCard.wrap(HideShopEntry.nitroCard)
            createRoute.wrap(HideShopEntry.createRoute)
        }
    }
}

private object HideShopEntry : ExtJsModule("discord-shop") {
    val nitroCard by export()
    val createRoute by export()
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
