// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.twitch.chat

import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patch.settings.valueOf
import app.reseam.patches.twitch.TwitchPatchStrings
import app.reseam.patches.twitch.core.TWITCH
import app.reseam.patches.twitch.core.TwitchSettingsPages
import app.reseam.patches.twitch.core.applyModeration
import app.reseam.patches.twitch.core.chatMessageRow
import app.reseam.patches.twitch.core.twitchSettings
import app.reseam.twitch.bindings.DeletedMessages
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val keepDeletedMessages = patch(TwitchPatchStrings.keepDeletedMessages) {
    description(TwitchPatchStrings.keepDeletedMessagesDescription)
    compatibleWith(TWITCH)
    settings(
        twitchSettings,
        section(
            TwitchSettingsPages.Chat,
            TwitchStrings.sectionChatMessages,
            ChatSettings.keepDeletedMessages,
            ChatSettings.showTimeoutLength,
        ),
    )

    execute {
        gate(ChatSettings.keepDeletedMessages) {
            // The chat store sets the action on a deleted line only when you moderate the channel, so the extension records each delete, timeout and ban.
            applyModeration.before { DeletedMessages.rememberModeration(param(0)) }
            chatMessageRow.before { param(0).assign(DeletedMessages.withModeration(param(0), valueOf(ChatSettings.showTimeoutLength))) }
            messageBody.before { param(0).assign(DeletedMessages.withModeratedBody(param(0))) }
        }
    }
}

// Chat rows and mod logs draw message text with this component, and only deleted messages come in with strikethrough on.
private val messageBody = function("messageBody") {
    name("MessageBody")
    strings("strikethrough", "moderatedRanges")
    paramCount(1)
}
