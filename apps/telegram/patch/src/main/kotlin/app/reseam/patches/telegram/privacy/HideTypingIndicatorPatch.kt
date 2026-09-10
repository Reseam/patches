// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.messagesController
import app.reseam.patches.telegram.core.telegramSettings

val hideTypingIndicator = patch("Hide typing indicator") {
    description("Don't notify others when you're typing or recording.")
    compatibleWith(TELEGRAM)
    settings(telegramSettings, section("Privacy", TelegramSettings.hideTyping))

    execute {
        sendTyping.returnFalseWhen(TelegramSettings.hideTyping)
    }
}

// Both sendTyping overloads route through the 5-arg one.
val sendTyping = messagesController.method("sendTyping") {
    params(Type.Long, Type.Long, Type.Int, Type.String, Type.Int)
}
