// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.returnFalseWhen
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
        shouldAnimateSticker.returnFalseWhen(DiscordSettings.staticStickers)
    }
}

private val shouldAnimateSticker = function {
    name("shouldAnimateSticker")
    strings("ANIMATE_ON_INTERACTION", "NEVER_ANIMATE")
    paramCount(2)
}
