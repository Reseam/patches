// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.field
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.core.DeletedArchive
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TL_MESSAGES
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.chatActivity
import app.reseam.patches.telegram.core.messagesController
import app.reseam.patches.telegram.core.messagesStorage
import app.reseam.patches.telegram.core.telegramSettings

val antiDelete = patch("Recover deleted messages") {
    description("Keeps messages others delete, archived locally and shown back in the chat.")
    compatibleWith(TELEGRAM)
    dependsOn(antiDeleteRuntime)
    settings(telegramSettings, section("Privacy", TelegramSettings.recoverDeleted))

    execute {
        val gate = TelegramSettings.recoverDeleted

        deleteMessages.before {
            call(DeletedArchive.markLocalDelete, param(3), param(0))
        }

        markMessagesAsDeleted.before(gate) {
            call(DeletedArchive.onMarkDeleted, thisObject, param(0), param(1))
        }

        processDeletedMessages.before(gate) {
            call(DeletedArchive.filterDeletedMessages, param(0), thisObject.field(messagesDict), thisObject.field(chatAdapter))
        }

        processLoadedMessages.before(gate) {
            call(DeletedArchive.injectDeleted, param(0), param(2))
        }

        chatOnResume.before(gate) {
            call(DeletedArchive.reapplyMarkers, thisObject.field(messagesDict), thisObject.field(chatAdapter))
        }
    }
}

val messagesDict = chatActivity.field("messagesDict")
val chatAdapter = chatActivity.field("chatAdapter")

val deleteMessages = messagesController.method("deleteMessages") {
    hasParam("org.telegram.tgnet.TLObject")
    rankBy("arity") { paramCount }
}

val markMessagesAsDeleted = messagesStorage.method("markMessagesAsDeletedInternal") {
    params(Type.Long, Type.ArrayList, Type.Boolean, Type.Int, Type.Int)
}

val processDeletedMessages = chatActivity.method("processDeletedMessages") {
    params(Type.ArrayList, Type.Long, Type.Boolean, Type.Boolean)
}

val processLoadedMessages = messagesController.method("processLoadedMessages") {
    param(0, TL_MESSAGES)
}

val chatOnResume = chatActivity.method("onResume") { params() }
