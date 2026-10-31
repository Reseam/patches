// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.Opcode
import app.reseam.patch.field
import app.reseam.patch.fieldOfType
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.core.SPARSE_ARRAYS
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.TL_MESSAGES
import app.reseam.patches.telegram.core.chatActivity
import app.reseam.patches.telegram.core.messagesController
import app.reseam.patches.telegram.core.messagesStorage
import app.reseam.patches.telegram.core.telegramSettings
import app.reseam.telegram.antidelete.bindings.DeletedArchive
import app.reseam.telegram.antidelete.bindings.MessageMarkers
import app.reseam.telegram.antidelete.bindings.ArchivedMessages
import app.reseam.telegram.antidelete.bindings.ArchiveStorage
import app.reseam.telegram.antidelete.bindings.RecoveredChats
import app.reseam.telegram.settings.TelegramSettings
import app.reseam.telegram.settings.TelegramStrings

val antiDelete = patch("Recover deleted messages") {
    description("Keeps messages others delete, archived locally and shown back in the chat.")
    compatibleWith(TELEGRAM)
    enabledByDefault(false)
    dependsOn(antiDeleteRuntime)
    settings(telegramSettings, section(TelegramStrings.sectionPrivacy, TelegramSettings.recoverDeleted, TelegramSettings.recoverDeletedChats))

    execute {
        // R8 renames RecyclerView; the chat adapter's own refresh is what ChatActivity calls.
        DeletedArchive.notifyDataSetChanged.implement {
            param(0).cast(chatAdapterRefresh.owner).call(chatAdapterRefresh, bool(false))
            returnVoid()
        }

        gate(TelegramSettings.recoverDeleted) {
            deleteMessages.before {
                DeletedArchive.markLocalDelete(param(3), param(0))
            }

            markMessagesAsDeleted.before {
                ArchiveStorage.onMarkDeleted(thisObject, param(0), param(1))
            }

            processDeletedMessages.before {
                param(0).assign(DeletedArchive.filterDeletedMessages(param(0), param(1), thisObject.field(chatDialogId), thisObject.field(messagesDict), thisObject.field(chatAdapter)))
            }

            processLoadedMessages.before {
                ArchivedMessages.injectDeleted(param(0), param(2), param(5), param(13))
            }

            chatOnResume.before {
                MessageMarkers.reapplyMarkers(thisObject.field(messagesDict), thisObject.field(chatAdapter), thisObject.field(chatDialogId))
            }

            chatCreateView.after {
                whenNotNull(thisObject.field(chatHeaderItem)) {
                    thisObject.field(chatHeaderItem).call(
                        addMenuItem,
                        int(RECOVER_DELETED_ITEM), staticField(recoverDeletedIcon), RecoveredChats.menuLabel(thisObject.field(chatDialogId)),
                    )
                }
            }

            chatMenuClick.before {
                whenEqual(param(0), int(RECOVER_DELETED_ITEM)) {
                    val chat = thisObject.fieldOfType(chatActivity.descriptor)
                    val label = RecoveredChats.toggle(chat.field(chatDialogId))
                    chat.field(chatHeaderItem).field(lazyMenuItems)
                        .callVirtual("java.util.HashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", callStatic("java.lang.Integer", "valueOf", "(I)Ljava/lang/Integer;", param(0)))
                        .cast(lazyMenuItem.descriptor)
                        .call(setMenuItemText, label)
                    returnVoid()
                }
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

// processDeletedMessages returns early unless channelId == -dialog_id; that read is the chat's dialog_id.
val chatDialogId = processDeletedMessages
    .point { opcode(Opcode.NEG_LONG) }
    .previous { opcode(Opcode.IGET_WIDE) }
    .field("dialogId")

// ChatActivity's own menu item IDs stay at or below 1000.
private const val RECOVER_DELETED_ITEM = 7_340_001

val chatCreateView = chatActivity.method("createView") { params(Type.Context) }

// The ⋮ menu gets its sub-menu delegate right after createView builds it.
val chatHeaderItem = chatCreateView
    .point { invokeVirtual { name("setSubMenuDelegate") } }
    .previous { opcode(Opcode.IPUT_OBJECT) }
    .field("headerItem")

val menuItem = classTarget("menuItem") {
    bytecode.findClass(chatHeaderItem.type) ?: error("The menu item class is missing")
}

// lazilyAddSubItem(id, icon, text) queues an item built when the menu first opens; it hands the
// item to putLazyItem, the one method of the menu item that takes and returns an item.
val addMenuItem = method("lazilyAddSubItem") {
    inClass(menuItem)
    params(Type.Int, Type.Int, Type.String)
    callsMethod { definingClass == menuItem.descriptor && proto.substringAfter('(').substringBefore(')') == proto.substringAfter(')') }
}

val lazyMenuItem = classTarget("lazyMenuItem") {
    bytecode.findClass(addMenuItem.returnType) ?: error("The lazy menu item class is missing")
}

// putLazyItem keys the queued items by ID.
val lazyMenuItems = menuItem.fieldOfType("java.util.HashMap")

val setMenuItemText = method("setMenuItemText") {
    inClass(lazyMenuItem)
    params(Type.CharSequence)
    returns(Type.Void)
}

private val recoverDeletedIcon = klass("org.telegram.messenger.R\$drawable").field("msg_reset")

// The action bar's click handler for ChatActivity's menus; the log line is its own.
val chatMenuClick = method("chatMenuClick") {
    strings("No rich message copied")
    params(Type.Int)
    returns(Type.Void)
}

// Params 5 and 13 are max_id and load_type, the range the page was requested for.
val processLoadedMessages = messagesController.method("processLoadedMessages") {
    param(0, TL_MESSAGES)
    param(5, Type.Int)
    param(13, Type.Int)
}

val chatOnResume = chatActivity.method("onResume") { params() }
