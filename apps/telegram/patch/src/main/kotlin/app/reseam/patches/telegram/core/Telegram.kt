// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.core

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.invoke
import app.reseam.patch.klass

val TELEGRAM = "org.telegram.messenger"("12.10.6")

const val TL_USER = "org.telegram.tgnet.TLRPC\$User"
const val TL_CHAT = "org.telegram.tgnet.TLRPC\$Chat"
const val TL_USER_FULL = "org.telegram.tgnet.TLRPC\$UserFull"
const val TL_MESSAGES = "org.telegram.tgnet.TLRPC\$messages_Messages"
const val MESSAGE_OBJECT = "org.telegram.messenger.MessageObject"
const val MESSAGE_SUGGESTION_PARAMS = "org.telegram.messenger.MessageSuggestionParams"
const val SEND_MESSAGE_PARAMS = "org.telegram.messenger.SendMessagesHelper\$SendMessageParams"
const val SPARSE_ARRAYS = "android.util.SparseArray[]"
const val ACTIVITY = "android.app.Activity"
const val DRAWABLE = "android.graphics.drawable.Drawable"
const val CLICK_LISTENER = "android.view.View\$OnClickListener"

val messagesController = klass("org.telegram.messenger.MessagesController")
val messagesStorage = klass("org.telegram.messenger.MessagesStorage")
val sendMessagesHelper = klass("org.telegram.messenger.SendMessagesHelper")
// R8 renames Telegram's UI classes; the plural key for the pinned-message count is ChatActivity's alone.
val chatActivity = klass("chatActivity") { strings("PinnedMessagesCount") }

object TelegramSettingsEntry : ExtClass("app.reseam.telegram.settings.TelegramSettingsEntry") {
    val init by static(Type.Context)
    val reseamItem by static(ACTIVITY, returns = Type.Object)
    val logo by static(Type.Context, returns = DRAWABLE)
    val opener by static(returns = CLICK_LISTENER)
}

object DeletedArchive : ExtClass("app.reseam.telegram.antidelete.DeletedArchive") {
    val init by static(Type.Context)
    val markLocalDelete by static(Type.Long, Type.ArrayList)
    val onMarkDeleted by static("org.telegram.messenger.MessagesStorage", Type.Long, Type.ArrayList)
    val filterDeletedMessages by static(Type.ArrayList, SPARSE_ARRAYS, Type.Object)
    val reapplyMarkers by static(SPARSE_ARRAYS, Type.Object)
    val injectDeleted by static(TL_MESSAGES, Type.Long)
    val notifyDataSetChanged by static(Type.Object)
}

object TelegramForwardBridge : ExtClass("app.reseam.telegram.forward.TelegramForwardBridge") {
    val tryFakeForward by static(
        Type.ArrayList, Type.Long, Type.Long, Type.Long, MESSAGE_SUGGESTION_PARAMS,
        returns = Type.Boolean,
    )
    val fixPathForNoForwards by static(SEND_MESSAGE_PARAMS)
}
