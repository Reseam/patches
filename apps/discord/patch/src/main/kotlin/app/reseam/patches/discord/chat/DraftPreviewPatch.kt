// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.discord.chat

import app.reseam.discord.bindings.DraftPreview
import app.reseam.discord.settings.DiscordSettings
import app.reseam.discord.settings.DiscordStrings
import app.reseam.patch.function
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.discord.core.DISCORD
import app.reseam.patches.discord.core.discordModules
import app.reseam.patches.discord.core.discordSettings

val draftPreview = patch("Draft preview") {
    description("Long press the send button to see your message in the chat before sending it.")
    compatibleWith(DISCORD)
    dependsOn(discordModules)
    settings(discordSettings, section(DiscordStrings.sectionChat, DiscordSettings.draftPreview))

    execute {
        gate(DiscordSettings.draftPreview) {
            chatInputActionButton.after {
                returnValue(DraftPreview.withLongPress(result, param(0)))
            }
            sendButton.after {
                returnValue(DraftPreview.sendButtonWithPreview(result, param(0)))
            }
            sendButtonItem.after {
                returnValue(DraftPreview.sendButtonItemWithPreview(result, param(0)))
            }
        }
    }
}

/** A round button beside the message box, such as send or attach. */
private val chatInputActionButton = function {
    name("")
    strings("CHAT_INPUT_ACTION_BUTTON_SIZE", "CHAT_INPUT_BUTTON_MIN_TOUCH_TARGET_SIZE", "activeIconStyle")
    paramCount(2)
}

/** The send button while voice messages are unavailable. */
private val sendButton = function {
    name("")
    strings("SendMessageIcon", "canSendVoiceMessage", "TransitionGroup")
    paramCount(2)
}

/** The send button as one item of the send and voice message transition. */
private val sendButtonItem = function {
    name("")
    strings("SendMessageIcon", "BUTTON_SEND_VOICE_MESSAGE", "withBounce")
    paramCount(1)
}
