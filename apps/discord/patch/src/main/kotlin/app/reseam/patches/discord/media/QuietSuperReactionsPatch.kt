// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.media

import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val quietSuperReactions = patch(DiscordPatchStrings.quietSuperReactions) {
    description(DiscordPatchStrings.quietSuperReactionsDescription)
    compatibleWith(DISCORD)
    settings(discordSettings, section(DiscordStrings.sectionMedia, DiscordSettings.quietSuperReactions))

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
