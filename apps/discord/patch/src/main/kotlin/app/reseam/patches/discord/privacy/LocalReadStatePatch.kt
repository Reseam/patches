// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.privacy

import app.reseam.discord.bindings.ReadState
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordSettings

val localReadState = patch("Local read state") {
    description("Mark messages as read on this phone without updating your other devices.")
    compatibleWith(DISCORD)
    enabledByDefault(false)
    settings(discordSettings, section(DiscordStrings.sectionPrivacy, DiscordSettings.localReadState))

    execute {
        gate(DiscordSettings.localReadState) {
            ack.before { param(0).assign(ReadState.localAck(param(0))) }
            handleBulkAck.before { param(0).assign(ReadState.localBulkAck(param(0))) }
        }
    }
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
