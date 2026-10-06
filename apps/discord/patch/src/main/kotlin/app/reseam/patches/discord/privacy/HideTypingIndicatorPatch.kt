// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.privacy

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.DiscordSettings
import app.reseam.patches.discord.core.discordSettings

val hideTypingIndicator = patch("Hide typing indicator") {
    description("Stop others from seeing when you are typing.")
    compatibleWith(DISCORD)
    settings(discordSettings, section("Privacy", DiscordSettings.hideTypingIndicator))

    execute {
        gate(DiscordSettings.hideTypingIndicator) {
            handleTypingStartLocal.alwaysReturn(false)
            sendTyping.alwaysReturn()
        }
    }
}

private val handleTypingStartLocal = function {
    name("handleTypingStartLocal")
    strings("FAKE_PLACEHOLDER_PRIVATE_CHANNEL_ID", "prevSend")
    paramCount(1)
}

private val sendTyping = function {
    name("")
    strings("TYPING", "HTTP", "timeout")
    paramCount(0)
}
