// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.privacy

import app.reseam.patch.ExtJsModule
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val localReadState = patch("Local read state") {
    description("Mark messages as read on this phone without updating your other devices.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section("Privacy", DiscordSettings.localReadState))

    execute {
        gate(DiscordSettings.localReadState) {
            ack.wrap(LocalReadState.ack)
            handleBulkAck.wrap(LocalReadState.handleBulkAck)
        }
    }
}

private object LocalReadState : ExtJsModule("discord-read-state") {
    val ack by export()
    val handleBulkAck by export()
}

private val ack = function {
    name("ack")
    strings("outgoingAckTimer", "_shouldAck", "local")
    paramCount(1)
}

private val handleBulkAck = function {
    name("handleBulkAck")
    strings("channels", "context", "onFinished")
    paramCount(1)
}
