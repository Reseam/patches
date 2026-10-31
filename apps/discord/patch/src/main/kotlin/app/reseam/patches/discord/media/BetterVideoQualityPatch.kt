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

val betterVideoQuality = patch("Better video quality") {
    description("Use a higher video quality when preparing uploads. Upload limits stay the same.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section(DiscordStrings.sectionMedia, DiscordSettings.betterVideoQuality))

    execute {
        gate(DiscordSettings.betterVideoQuality) {
            canUseHighVideoUploadQuality.alwaysReturn(true)
        }
    }
}

private val canUseHighVideoUploadQuality = function {
    name("canUseHighVideoUploadQuality")
    strings("INCREASED_VIDEO_UPLOAD_QUALITY", "canUserUse")
    paramCount(1)
}
