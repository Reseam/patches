// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.TelegramPatchStrings
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.telegram.settings.TelegramSettings
import app.reseam.patches.telegram.core.messagesController
import app.reseam.patches.telegram.core.telegramSettings
import app.reseam.telegram.settings.TelegramStrings

val hideTypingIndicator = patch(TelegramPatchStrings.hideTypingIndicator) {
    description(TelegramPatchStrings.hideTypingIndicatorDescription)
    compatibleWith(TELEGRAM)
    settings(telegramSettings, section(TelegramStrings.sectionPrivacy, TelegramSettings.hideTyping))

    execute {
        gate(TelegramSettings.hideTyping) {
            sendTyping.alwaysReturn(false)
        }
    }
}

// Both sendTyping overloads route through the 5-arg one.
val sendTyping = messagesController.method("sendTyping") {
    params(Type.Long, Type.Long, Type.Int, Type.String, Type.Int)
}
