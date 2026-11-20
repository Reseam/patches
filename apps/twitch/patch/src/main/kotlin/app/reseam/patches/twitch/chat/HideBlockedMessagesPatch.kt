// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.chat

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.chatMessageRow
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.bindings.BlockedUsers
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val hideBlockedMessages = patch(TwitchPatchStrings.hideBlockedMessages) {
    description(TwitchPatchStrings.hideBlockedMessagesDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionChatMessages, ChatSettings.hideBlockedMessages))

    execute {
        // A line can turn hidden while it is on screen, so the row runs its React hooks first.
        gate(ChatSettings.hideBlockedMessages) {
            chatMessageRow.after { whenTrue(BlockedUsers.isHiddenMessage(param(0))) { returnNull() } }
        }
    }
}
