// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.privacy

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val blockAnalytics = patch("Block analytics") {
    description("Stop Discord from logging usage events.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Privacy", DiscordSettings.blockAnalytics))

    execute {
        gate(DiscordSettings.blockAnalytics) {
            handleTrack.alwaysReturn()
        }
    }
}

private val handleTrack = function {
    name("handleTrack")
    strings("event", "properties", "fingerprint", "isDeveloper")
    paramCount(1)
}
