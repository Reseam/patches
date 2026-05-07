// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.before
import app.reseam.patch.compatibleWith
import app.reseam.patch.patch
import app.reseam.patch.settings.prependWhen
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.settingsPatch

/**
 * Server enforces `noforwards` on `messages.forwardMessages` API calls — a client return-false
 * hook on `isChatNoForwards` only re-enables the UI buttons. To actually deliver, route forwards
 * from no-forwards chats through `SendMessagesHelper.processForwardFromMyName`, which the app
 * already uses for encrypted dialogs: it re-sends each message via `messages.sendMedia` /
 * `sendMessage` as new uploads, so the server-side forward block doesn't apply.
 */
val forwardFromRestrictedPatch = patch(
    name = "Forward from restricted chats",
    description = "Re-sends as a new message when forwarding from a no-forwards chat.",
    compatibleWith = listOf(compatibleWith("org.telegram.messenger", "12.7.1")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch),
) {
    extendWith("telegram-settings.dex")
    execute { ctx ->
        val proto =
            "(Ljava/util/ArrayList;JZZZIILorg/telegram/messenger/MessageObject;" +
                "IJJLorg/telegram/messenger/MessageSuggestionParams;)I"
        val sendMessage = ctx.bytecode.findClass("Lorg/telegram/messenger/SendMessagesHelper;")
            ?.methods?.firstOrNull {
                it.info.methodName == "sendMessage" && it.info.proto == proto
            } ?: error("SendMessagesHelper.sendMessage(ArrayList,...)I not found")

        sendMessage.prependWhen(TelegramSettings.SaveFromRestricted) {
            val handled = staticCall(
                "Lapp/reseam/telegram/forward/TelegramForwardBridge;",
                "tryFakeForward",
                "(Ljava/util/ArrayList;JJJLorg/telegram/messenger/MessageSuggestionParams;)Z",
                parameter(0),                              // messages
                parameter(1),                              // peer
                parameter(9),                              // payStars
                parameter(10),                             // monoForumPeerId
                parameter(11),                             // suggestionParams
            )
            ifTrue(handled) {
                // Method returns int; const-0 + return is a valid int return on any 32-bit type.
                returnFalse()
            }
        }

        // The redirect routes through processForwardFromMyName, which builds SendMessageParams
        // with `path = null` for photos and an unset attachPath for received documents. That
        // makes the eventual `messages.sendMedia` reference the server-side media id of a
        // no-forwards source, and the server rejects it. Set `path` to the local cache so the
        // send re-uploads instead.
        val sendOne = ctx.bytecode.findClass("Lorg/telegram/messenger/SendMessagesHelper;")
            ?.methods?.firstOrNull {
                it.info.methodName == "sendMessage" &&
                    it.info.proto == "(Lorg/telegram/messenger/SendMessagesHelper\$SendMessageParams;)V"
            } ?: error("SendMessagesHelper.sendMessage(SendMessageParams)V not found")
        sendOne.before {
            staticCall(
                "Lapp/reseam/telegram/forward/TelegramForwardBridge;",
                "fixPathForNoForwards",
                "(Lorg/telegram/messenger/SendMessagesHelper\$SendMessageParams;)V",
                parameter(0),
            )
        }
    }
}
