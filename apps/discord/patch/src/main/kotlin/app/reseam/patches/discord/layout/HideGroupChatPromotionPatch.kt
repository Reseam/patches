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

val hideGroupChatPromotion = patch("Hide group chat promotion") {
    description("Remove the Nitro recipient limit promotion from group chats.")
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionLayout, DiscordSettings.hideGroupChatPromotion))

    execute {
        gate(DiscordSettings.hideGroupChatPromotion) {
            groupDmNitroUpsellBanner.alwaysReturnNull()
        }
    }
}

private val groupDmNitroUpsellBanner = function {
    name("GroupDMNitroUpsellBanner")
    strings("GroupDMNitroAcquisitionStrategy", "onFloatingListInsetChange")
    paramCount(1)
}
