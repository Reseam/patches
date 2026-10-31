// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val betterStreamQuality = patch("Better stream quality") {
    description("Stream your screen at the resolutions and frame rates Nitro unlocks.")
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionMedia, DiscordSettings.betterStreamQuality))

    execute {
        gate(DiscordSettings.betterStreamQuality) {
            canStreamQuality.alwaysReturn(true)
        }
    }
}

private val canStreamQuality = function {
    name("canStreamQuality")
    strings("STREAM_HIGH_QUALITY", "STREAM_MID_QUALITY", "canUserUse")
    paramCount(2)
}
