// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patches.telegram.core.MESSAGE_OBJECT
import app.reseam.patches.telegram.core.MESSAGE_SUGGESTION_PARAMS
import app.reseam.patches.telegram.core.SEND_MESSAGE_PARAMS
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TelegramForwardBridge
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.sendMessagesHelper
import app.reseam.patches.telegram.core.telegramSettings

// The server enforces `noforwards` on messages.forwardMessages, so re-enabling the UI is not
// enough. Forwards from such chats go through processForwardFromMyName, which the app already
// uses for encrypted dialogs: each message is re-sent as a fresh upload.
val forwardFromRestricted = patch("Forward from restricted chats") {
    description("Re-sends as a new message when forwarding from a no-forwards chat.")
    compatibleWith(TELEGRAM)
    dependsOn(telegramSettings)

    execute {
        sendMessages.before(TelegramSettings.saveFromRestricted) {
            val handled = call(TelegramForwardBridge.tryFakeForward, param(0), param(1), param(9), param(10), param(11))
            whenTrue(handled) {
                returnValue(int(0))
            }
        }

        // processForwardFromMyName leaves `path` unset for received media, which makes the
        // eventual messages.sendMedia reference a server-side media id the server rejects.
        // Pointing it at the local cache makes the send re-upload instead.
        sendOneMessage.before {
            call(TelegramForwardBridge.fixPathForNoForwards, param(0))
        }
    }
}

val sendMessages = sendMessagesHelper.method("sendMessage") {
    params(
        Type.ArrayList, Type.Long, Type.Boolean, Type.Boolean, Type.Boolean, Type.Int, Type.Int,
        MESSAGE_OBJECT, Type.Int, Type.Long, Type.Long, MESSAGE_SUGGESTION_PARAMS,
    )
}

val sendOneMessage = sendMessagesHelper.method("sendMessage") { params(SEND_MESSAGE_PARAMS) }
