// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val hideContactSyncPrompt = patch(DiscordPatchStrings.hideContactSyncPrompt) {
    description(DiscordPatchStrings.hideContactSyncPromptDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionLayout, DiscordSettings.hideContactSyncPrompt))

    execute {
        gate(DiscordSettings.hideContactSyncPrompt) {
            contactSyncUpsellCta.alwaysReturnNull()
        }
    }
}

private val contactSyncUpsellCta = function {
    name("ContactSyncUpsellCTA")
    strings("FormCTA", "location", "container")
    paramCount(1)
}
