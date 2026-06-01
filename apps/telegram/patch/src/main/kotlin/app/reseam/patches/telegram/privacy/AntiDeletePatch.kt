// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.telegram.privacy

import app.reseam.patch.FieldRef
import app.reseam.patch.before
import app.reseam.patch.classesExtending
import app.reseam.patch.compatibleWith
import app.reseam.patch.patch
import app.reseam.patch.settings.SettingsSection
import app.reseam.patch.settings.prependWhen
import app.reseam.patch.wrapField
import app.reseam.patches.telegram.core.TelegramSettings
import app.reseam.patches.telegram.core.settingsPatch

private const val ARCHIVE = "Lapp/reseam/telegram/antidelete/DeletedArchive;"

val antiDeletePatch = patch(
    name = "Recover deleted messages",
    description = "Keeps messages others delete, archived locally and shown back in the chat.",
    compatibleWith = listOf(compatibleWith("org.telegram.messenger", "12.7.1")),
    settingsHost = settingsPatch,
    dependsOn = listOf(settingsPatch),
    settings = listOf(
        SettingsSection("Privacy", listOf(TelegramSettings.RecoverDeleted)),
    ),
) {
    extendWith("telegram-anti-delete.dex")
    execute { ctx ->
        val mc = ctx.bytecode.findClass("Lorg/telegram/messenger/MessagesController;")
            ?: error("MessagesController class not found")
        val ca = ctx.bytecode.findClass("Lorg/telegram/ui/ChatActivity;")
            ?: error("ChatActivity class not found")

        val messagesDict = ctx.wrapField(
            ca.instanceFields.first { it.name == "messagesDict" }.let {
                FieldRef(it.classDescriptor, it.name, it.fieldType)
            },
        )
        val chatAdapter = ctx.wrapField(
            ca.instanceFields.first { it.name == "chatAdapter" }.let {
                FieldRef(it.classDescriptor, it.name, it.fieldType)
            },
        )

        val deleteMessages = mc.methods.filter {
            it.info.methodName == "deleteMessages" &&
                it.info.proto.contains("Lorg/telegram/tgnet/TLObject;")
        }.maxByOrNull { it.info.proto.length }
            ?: error("MessagesController.deleteMessages(12-arg) not found")
        deleteMessages.before {
            staticCall(
                ARCHIVE, "markLocalDelete", "(JLjava/util/ArrayList;)V",
                parameter(3), parameter(0),
            )
        }

        val storage = ctx.bytecode.findClass("Lorg/telegram/messenger/MessagesStorage;")
            ?: error("MessagesStorage class not found")
        val markDeleted = storage.methods.firstOrNull {
            it.info.methodName == "markMessagesAsDeletedInternal" &&
                it.info.proto == "(JLjava/util/ArrayList;ZII)Ljava/util/ArrayList;"
        } ?: error("MessagesStorage.markMessagesAsDeletedInternal not found")
        markDeleted.prependWhen(TelegramSettings.RecoverDeleted) {
            staticCall(
                ARCHIVE, "onMarkDeleted",
                "(Lorg/telegram/messenger/MessagesStorage;JLjava/util/ArrayList;)V",
                thisObject(), parameter(0), parameter(1),
            )
        }

        val processDeleted = ca.methods.firstOrNull {
            it.info.methodName == "processDeletedMessages" &&
                it.info.proto == "(Ljava/util/ArrayList;JZZ)V"
        } ?: error("ChatActivity.processDeletedMessages not found")
        processDeleted.prependWhen(TelegramSettings.RecoverDeleted) {
            staticCall(
                ARCHIVE, "filterDeletedMessages",
                "(Ljava/util/ArrayList;[Landroid/util/SparseArray;Ljava/lang/Object;)V",
                parameter(0),
                thisObject().field(messagesDict),
                thisObject().field(chatAdapter),
            )
        }

        val processLoaded = mc.methods.firstOrNull {
            it.info.methodName == "processLoadedMessages" &&
                it.info.proto.startsWith("(Lorg/telegram/tgnet/TLRPC\$messages_Messages;")
        } ?: error("MessagesController.processLoadedMessages not found")
        processLoaded.prependWhen(TelegramSettings.RecoverDeleted) {
            staticCall(
                ARCHIVE, "injectDeleted",
                "(Lorg/telegram/tgnet/TLRPC\$messages_Messages;J)V",
                parameter(0), parameter(2),
            )
        }

        val onResume = ca.methods.firstOrNull {
            it.info.methodName == "onResume" && it.info.proto == "()V"
        } ?: error("ChatActivity.onResume not found")
        onResume.prependWhen(TelegramSettings.RecoverDeleted) {
            staticCall(
                ARCHIVE, "reapplyMarkers",
                "([Landroid/util/SparseArray;Ljava/lang/Object;)V",
                thisObject().field(messagesDict),
                thisObject().field(chatAdapter),
            )
        }

        var hooked = 0
        for (cls in ctx.bytecode.classesExtending("Landroid/app/Application;")) {
            val onCreate = cls.methods.firstOrNull {
                it.info.methodName == "onCreate" && it.info.proto == "()V"
            } ?: continue
            onCreate.before {
                staticCall(ARCHIVE, "init", "(Landroid/content/Context;)V", thisObject())
            }
            hooked++
        }
        if (hooked == 0) ctx.log.warn("No Application subclass hooked")
    }
}
