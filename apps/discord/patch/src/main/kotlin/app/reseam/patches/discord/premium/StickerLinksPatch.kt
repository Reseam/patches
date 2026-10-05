// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.premium

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patch.settings.wrapWhen
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val stickerLinks = patch("Sticker links") {
    description("Send locked PNG, APNG and GIF stickers as image links.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Nitro", DiscordSettings.stickerLinks))

    execute {
        getStickerSendability.wrapWhen(DiscordSettings.stickerLinks, StickerLinks.getStickerSendability)
        getStickerAssetUrl.wrapWhen(DiscordSettings.stickerLinks, StickerLinks.getStickerAssetUrl)
        sendStickers.wrapWhen(DiscordSettings.stickerLinks, StickerLinks.sendStickers)
    }
}

private object StickerLinks : ExtJsModule("discord-sticker-links") {
    val getStickerSendability = export("getStickerSendability")
    val getStickerAssetUrl = export("getStickerAssetUrl")
    val sendStickers = export("sendStickers")
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
