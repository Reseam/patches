// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.privacy

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val blockCrashReports = patch(DiscordPatchStrings.blockCrashReports) {
    description(DiscordPatchStrings.blockCrashReportsDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionPrivacy, DiscordSettings.blockCrashReports))

    execute {
        gate(DiscordSettings.blockCrashReports) {
            isDisabled.alwaysReturn(true)
        }
    }
}

private val isDisabled = klass("com.discord.crash_reporting.CrashReporting").method("isDisabled") {
    params()
    returns(Type.Boolean)
}
