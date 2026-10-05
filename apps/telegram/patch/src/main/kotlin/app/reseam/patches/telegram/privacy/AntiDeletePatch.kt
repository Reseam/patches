// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.Opcode
import app.reseam.patch.field
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.core.DeletedArchive
import app.reseam.patches.telegram.core.SPARSE_ARRAYS
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
        deleteMessages.before {
            call(DeletedArchive.markLocalDelete, param(3), param(0))
        }

        // R8 renames RecyclerView; the chat adapter's own refresh is what ChatActivity calls.
        DeletedArchive.notifyDataSetChanged.implement {
            param(0).cast(chatAdapterRefresh.owner).call(chatAdapterRefresh, bool(false))
            returnVoid()
        }

        gate(TelegramSettings.recoverDeleted) {
            markMessagesAsDeleted.before {
                call(DeletedArchive.onMarkDeleted, thisObject, param(0), param(1))
            }

            processDeletedMessages.before {
                call(DeletedArchive.filterDeletedMessages, param(0), thisObject.field(messagesDict), thisObject.field(chatAdapter))
            }

            processLoadedMessages.before {
                call(DeletedArchive.injectDeleted, param(0), param(2))
            }

            chatOnResume.before {
                call(DeletedArchive.reapplyMarkers, thisObject.field(messagesDict), thisObject.field(chatAdapter))
            }
        }
    }
}

val deleteMessages = messagesController.method("deleteMessages") {
    hasParam("org.telegram.tgnet.TLObject")
    rankBy("arity") { paramCount }
}

val markMessagesAsDeleted = messagesStorage.method("markMessagesAsDeletedInternal") {
    params(Type.Long, Type.ArrayList, Type.Boolean, Type.Int, Type.Int)
}

val processDeletedMessages = method("processDeletedMessages") {
    inClass(chatActivity)
    strings("PinnedMessagesCount")
    params(Type.ArrayList, Type.Long, Type.Boolean, Type.Boolean)
}

// The first message-map lookup in processDeletedMessages reads messagesDict.
val messagesDict = processDeletedMessages
    .point { opcode(Opcode.IGET_OBJECT); field { type(SPARSE_ARRAYS) } }
    .field("messagesDict")

// ChatActivityAdapter.notifyDataSetChanged(boolean) logs this before refreshing.
val chatAdapterRefresh = method("chatAdapterRefresh") {
    strings("notify data set changed fragmentOpened=")
    params(Type.Boolean)
}

val chatAdapter = processDeletedMessages
    .point { opcode(Opcode.IGET_OBJECT); field { owner(chatActivity.descriptor); type(chatAdapterRefresh.owner) } }
    .field("chatAdapter")

val processLoadedMessages = messagesController.method("processLoadedMessages") {
    param(0, TL_MESSAGES)
}

val chatOnResume = chatActivity.method("onResume") { params() }
