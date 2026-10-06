// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val staticStickers = patch("Static stickers") {
    description("Use still sticker previews and chat images.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Media", DiscordSettings.staticStickers))

    execute {
        gate(DiscordSettings.staticStickers) {
            shouldAnimateSticker.alwaysReturn(false)
        }
    }
}

private val shouldAnimateSticker = function {
    name("shouldAnimateSticker")
    strings("ANIMATE_ON_INTERACTION", "NEVER_ANIMATE")
    paramCount(2)
}
