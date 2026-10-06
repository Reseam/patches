// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.premium

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val stickerLinks = patch("Sticker links") {
    description("Send locked PNG, APNG and GIF stickers as image links.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Nitro", DiscordSettings.stickerLinks))

    execute {
        gate(DiscordSettings.stickerLinks) {
            getStickerSendability.wrap(StickerLinks.getStickerSendability)
            getStickerAssetUrl.wrap(StickerLinks.getStickerAssetUrl)
            sendStickers.wrap(StickerLinks.sendStickers)
        }
    }
}

private object StickerLinks : ExtJsModule("discord-sticker-links") {
    val getStickerSendability by export()
    val getStickerAssetUrl by export()
    val sendStickers by export()
}

private val getStickerSendability = function {
    name("getStickerSendability")
    strings("SENDABLE_WITH_PREMIUM", "USE_EXTERNAL_STICKERS")
    paramCount(3)
}

private val getStickerAssetUrl = function {
    name("")
    strings("STICKER_ASSET", "getBestMediaProxySize", "getForceSdrEmojisStickersConfig")
    paramCount(1)
}

private val sendStickers = function {
    name("sendStickers")
    strings("_sendMessage", "stickerIds")
    paramCount(2)
}
