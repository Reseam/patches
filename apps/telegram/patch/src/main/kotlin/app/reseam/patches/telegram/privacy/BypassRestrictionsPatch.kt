// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.returnFalseWhen
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TL_CHAT
import app.reseam.patches.telegram.core.TL_USER_FULL
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.messagesController
import app.reseam.patches.telegram.core.telegramSettings

val bypassRestrictions = patch("Save from restricted chats") {
    description("Re-enables copy, save, and forward in chats with content protection on.")
    compatibleWith(TELEGRAM)
    settings(telegramSettings, section("Privacy", TelegramSettings.saveFromRestricted))

    execute {
        noForwardGates.forEach { it.returnFalseWhen(TelegramSettings.saveFromRestricted) }
    }
}

// Every UI gate that hides save/copy/forward delegates to one of these.
val noForwardGates = listOf(
    messagesController.method("isChatNoForwards") { params(TL_CHAT) },
    messagesController.method("isChatNoForwards") { params(Type.Long) },
    messagesController.method("isUserNoForwards") { params(Type.Long) },
    messagesController.method("isUserNoForwards") { params(TL_USER_FULL) },
    messagesController.method("isPeerNoForwards") { params(Type.Long) },
)
