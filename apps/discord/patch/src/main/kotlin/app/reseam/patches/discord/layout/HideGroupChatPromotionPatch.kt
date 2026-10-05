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

val hideGroupChatPromotion = patch("Hide group chat promotion") {
    description("Remove the Nitro recipient limit promotion from group chats.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Layout", DiscordSettings.hideGroupChatPromotion))

    execute {
        groupDmNitroUpsellBanner.returnNullWhen(DiscordSettings.hideGroupChatPromotion)
    }
}

private val groupDmNitroUpsellBanner = function {
    name("GroupDMNitroUpsellBanner")
    strings("GroupDMNitroAcquisitionStrategy", "onFloatingListInsetChange")
    paramCount(1)
}
