// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val quietSuperReactions = patch("Quiet Super Reactions") {
    description("Remove looping Super Reaction decorations.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Media", DiscordSettings.quietSuperReactions))

    execute {
        gate(DiscordSettings.quietSuperReactions) {
            superReactionLocalImageAnimation.alwaysReturnNull()
        }
    }
}

private val superReactionLocalImageAnimation = function {
    name("SuperReactionLocalImageAnimation")
    strings("useSuperReactionAnimationSourceFromLocalImage", "animationSource")
    paramCount(1)
}
