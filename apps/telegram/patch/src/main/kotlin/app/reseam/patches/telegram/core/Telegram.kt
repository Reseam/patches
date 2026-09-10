// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.core

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.invoke
import app.reseam.patch.klass

val TELEGRAM = "org.telegram.messenger"("12.7.1")

const val TL_USER = "org.telegram.tgnet.TLRPC\$User"
const val TL_CHAT = "org.telegram.tgnet.TLRPC\$Chat"
const val TL_USER_FULL = "org.telegram.tgnet.TLRPC\$UserFull"
const val TL_MESSAGES = "org.telegram.tgnet.TLRPC\$messages_Messages"
const val MESSAGE_OBJECT = "org.telegram.messenger.MessageObject"
const val MESSAGE_SUGGESTION_PARAMS = "org.telegram.messenger.MessageSuggestionParams"
const val SEND_MESSAGE_PARAMS = "org.telegram.messenger.SendMessagesHelper\$SendMessageParams"
const val SPARSE_ARRAYS = "android.util.SparseArray[]"

val messagesController = klass("org.telegram.messenger.MessagesController")
val messagesStorage = klass("org.telegram.messenger.MessagesStorage")
val sendMessagesHelper = klass("org.telegram.messenger.SendMessagesHelper")
val chatActivity = klass("org.telegram.ui.ChatActivity")

object TelegramSettingsEntry : ExtClass("app.reseam.telegram.settings.TelegramSettingsEntry") {
    val init = static("init", Type.Context)
    val appendReseamItem = static("appendReseamItem", Type.ArrayList, "org.telegram.ui.ActionBar.BaseFragment")
}

object DeletedArchive : ExtClass("app.reseam.telegram.antidelete.DeletedArchive") {
    val init = static("init", Type.Context)
    val stripSecureFlag = static("stripSecureFlag", "android.view.WindowManager\$LayoutParams")
    val markLocalDelete = static("markLocalDelete", Type.Long, Type.ArrayList)
    val onMarkDeleted = static("onMarkDeleted", "org.telegram.messenger.MessagesStorage", Type.Long, Type.ArrayList)
    val filterDeletedMessages = static("filterDeletedMessages", Type.ArrayList, SPARSE_ARRAYS, Type.Object)
    val reapplyMarkers = static("reapplyMarkers", SPARSE_ARRAYS, Type.Object)
    val injectDeleted = static("injectDeleted", TL_MESSAGES, Type.Long)
}

object TelegramForwardBridge : ExtClass("app.reseam.telegram.forward.TelegramForwardBridge") {
    val tryFakeForward = static(
        "tryFakeForward",
        Type.ArrayList, Type.Long, Type.Long, Type.Long, MESSAGE_SUGGESTION_PARAMS,
        returns = Type.Boolean,
    )
    val fixPathForNoForwards = static("fixPathForNoForwards", SEND_MESSAGE_PARAMS)
}
