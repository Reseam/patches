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
import app.reseam.twitch.bindings.ChatModes
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val hideUniqueChatBanner = patch(TwitchPatchStrings.hideUniqueChatBanner) {
    description(TwitchPatchStrings.hideUniqueChatBannerDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionChatButtons, ChatSettings.hideUniqueChatBanner))

    execute {
        // The tray switches between chat modes in place, so it runs its React hooks first.
        gate(ChatSettings.hideUniqueChatBanner) {
            chatModeTray.after { whenTrue(ChatModes.isUniqueChat(param(0))) { returnNull() } }
        }
    }
}

// Shows the chat mode that matters most above the chat field, such as a suspension, slow mode or Unique Chat.
private val chatModeTray = function {
    name("ChatModeTray")
    paramCount(1)
}
