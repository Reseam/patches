// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.bindings.FirstMessage
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordModules
import app.reseam.patches.discord.core.discordSettings

val jumpToFirstMessage = patch("Jump to first message") {
    description("Add a button above Jump to present that jumps to the first message.")
    compatibleWith(DISCORD)
    dependsOn(discordModules)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.jumpToFirstMessage))

    execute {
        gate(DiscordSettings.jumpToFirstMessage) {
            jumpToPresentButton.after {
                returnValue(FirstMessage.withFirstMessageButton(result, param(0)))
            }
        }
    }
}

/** The arrow shown at the bottom of a chat scrolled away from the newest messages. */
private val jumpToPresentButton = function {
    name("JumpToPresentButton")
    strings("onJumpToPresent", "MemoedVoicePanelDismissChatButton")
    paramCount(1)
}
