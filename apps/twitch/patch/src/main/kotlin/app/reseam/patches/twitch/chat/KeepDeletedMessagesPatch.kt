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
import app.reseam.twitch.bindings.DeletedMessages
import app.reseam.twitch.settings.ChatSettings
import app.reseam.twitch.settings.TwitchStrings

val keepDeletedMessages = patch(TwitchPatchStrings.keepDeletedMessages) {
    description(TwitchPatchStrings.keepDeletedMessagesDescription)
    compatibleWith(TWITCH)
    settings(twitchSettings, section(TwitchSettingsPages.Chat, TwitchStrings.sectionChatMessages, ChatSettings.keepDeletedMessages))

    execute {
        gate(ChatSettings.keepDeletedMessages) {
            applyModeration.before { DeletedMessages.rememberModeration(param(0)) }
            chatMessageRow.before { param(0).assign(DeletedMessages.withModeration(param(0))) }
            messageBody.before { param(0).assign(DeletedMessages.withModeratedBody(param(0))) }
        }
    }
}

// The chat store sets the action on a deleted line only when you moderate the channel, so the extension records each delete, timeout and ban here.
private val applyModeration = function("applyModeration") {
    name("applyModeration")
    strings("BRIEF", "DETAILED")
    paramCount(1)
}

private val chatMessageRow = function("chatMessageRow") {
    name("ChatMessageRowComponent")
    strings("useIsMessageDeleted", "modActionType")
    paramCount(1)
}

// Chat rows and mod logs draw message text with this component, and only deleted messages come in with strikethrough on.
private val messageBody = function("messageBody") {
    name("MessageBody")
    strings("strikethrough", "moderatedRanges")
    paramCount(1)
}
