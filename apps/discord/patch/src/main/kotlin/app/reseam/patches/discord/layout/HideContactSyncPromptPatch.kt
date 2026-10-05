// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.layout

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.returnNullWhen
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val hideContactSyncPrompt = patch("Hide contact sync prompt") {
    description("Remove the contact sync promotion from the friends screen.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Layout", DiscordSettings.hideContactSyncPrompt))

    execute {
        contactSyncUpsellCta.returnNullWhen(DiscordSettings.hideContactSyncPrompt)
    }
}

private val contactSyncUpsellCta = function {
    name("ContactSyncUpsellCTA")
    strings("FormCTA", "location", "container")
    paramCount(1)
}
