// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.chat

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val hideWelcomeMessage = patch(TwitchPatchStrings.hideWelcomeMessage) {
    description(TwitchPatchStrings.hideWelcomeMessageDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionChatMessages, ChatSettings.hideWelcomeMessage))

    execute {
        gate(ChatSettings.hideWelcomeMessage) { chatConnectedLine.alwaysReturnNull() }
    }
}

// Chat writes Welcome to the chat room with this line when it connects.
private val chatConnectedLine = function("chatConnectedLine") {
    name("ChatConnectedLine")
    strings("StatusLine", "channelLogin")
    paramCount(1)
}
