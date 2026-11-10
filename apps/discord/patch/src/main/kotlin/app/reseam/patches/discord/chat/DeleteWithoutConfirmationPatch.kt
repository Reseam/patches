// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.bindings.DeleteWithoutConfirmation
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.DiscordPatchStrings
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordModules
import app.reseam.patches.discord.core.discordSettings

val deleteWithoutConfirmation = patch(DiscordPatchStrings.deleteWithoutConfirmation) {
    description(DiscordPatchStrings.deleteWithoutConfirmationDescription)
    compatibleWith(DISCORD)
    dependsOn(discordModules)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.deleteWithoutConfirmation))

    execute {
        gate(DiscordSettings.deleteWithoutConfirmation) {
            longPressMessageOptionHandler.before {
                whenTrue(DeleteWithoutConfirmation.deleteMessage(param(0))) { returnUndefined() }
            }
        }
    }
}

/** Runs the option picked from a message's long press menu, by its label. */
private val longPressMessageOptionHandler = function {
    name("longPressMessageOptionHandler")
    strings("onActionExecuted", "hideActionSheet", "deleteMessage")
    paramCount(1)
}
