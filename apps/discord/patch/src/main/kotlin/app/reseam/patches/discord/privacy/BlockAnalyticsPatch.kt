// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.privacy

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val blockAnalytics = patch(DiscordPatchStrings.blockAnalytics) {
    description(DiscordPatchStrings.blockAnalyticsDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionPrivacy, DiscordSettings.blockAnalytics))

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
