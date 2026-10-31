// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.telegram.core

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
