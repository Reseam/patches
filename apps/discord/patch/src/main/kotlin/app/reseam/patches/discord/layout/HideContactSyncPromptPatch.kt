// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val hideContactSyncPrompt = patch("Hide contact sync prompt") {
    description("Remove the contact sync promotion from the friends screen.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Layout", DiscordSettings.hideContactSyncPrompt))

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
