// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.telegram.messages

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.telegram.TelegramPatchStrings
import app.reseam.patches.telegram.core.MESSAGE_OBJECT
import app.reseam.patches.telegram.core.TELEGRAM
import app.reseam.patches.telegram.core.chatActivity
import app.reseam.patches.telegram.core.telegramSettings
import app.reseam.telegram.messages.bindings.MessageDetails
import app.reseam.telegram.settings.TelegramSettings
import app.reseam.telegram.settings.TelegramStrings

private const val R_STRING = "org.telegram.messenger.R\$string"

val customizeMessages = patch(TelegramPatchStrings.customizeMessages) {
    description(TelegramPatchStrings.customizeMessagesDescription)
    compatibleWith(TELEGRAM)
    settings(
        telegramSettings,
        section(
            TelegramStrings.sectionMessages,
            TelegramSettings.showMessageIds,
            TelegramSettings.showUserIds,
            TelegramSettings.showForwardDates,
            TelegramSettings.editedMark,
            TelegramSettings.customEditedMark,
        ),
    )

    execute {
        val copyIcon = resources.id("drawable", "msg_copy")?.toInt() ?: error("drawable/msg_copy missing")

        editedFlag.captureAs("edited", Type.Boolean).after {
            capture("edited").assign(MessageDetails.showsEdited(capture("edited")))
        }
        editDateFormat.captureAs("editDate", Type.Boolean).after {
            capture("editDate").assign(MessageDetails.showsEditDate(capture("editDate")))
        }
        editedLabel.captureAs("label", Type.String).after {
            capture("label").assign(MessageDetails.editedLabel(capture("label")))
        }
        timeDecorations.before {
            thisObject.set(timeText, MessageDetails.timeText(thisObject.field(timeText), param(0)))
        }
        forwardedFromLabel.after { returnValue(MessageDetails.forwardedLabel(result, param(0))) }
        messageMenuOptions.after { MessageDetails.addUserId(param(0), param(1), param(2), param(3), int(copyIcon)) }
        processSelectedOption.before { MessageDetails.onOption(param(0), thisObject.field(selectedMessage)) }
    }
}

// ChatMessageCell's measureTime builds the time and edited mark every bubble shows.
private val measureTime = method("measureTime") {
    params(MESSAGE_OBJECT)
    returns(Type.Void)
    accesses { owner(R_STRING); name("EditedMessage") }
}

// Read where the edited label would go; false takes the plain time.
private val editedFlag = measureTime
    .point { field { owner("org.telegram.messenger.AppGlobalConfig"); name("messagePrimaryEditedDate") } }
    .previous { opcode(Opcode.IGET_BOOLEAN) }

// Telegram's server config can show the edit date in a sentence instead of a label before the time.
private val editDateFormat = measureTime
    .point { field { owner("org.telegram.messenger.AppGlobalConfig"); name("messagePrimaryEditedDate") } }
    .next { resultOf(Type.Boolean) }

private val editedLabel = measureTime
    .point { field { owner(R_STRING); name("EditedMessage") } }
    .next { resultOf(Type.String) }

// Every way of building the time joins here, before the extras drawn next to it.
private val timeDecorations = measureTime.point { invokeVirtual { owner(MESSAGE_OBJECT); name("isStakedDice") } }
private val timeText = timeDecorations.previous { opcode(Opcode.IPUT_OBJECT) }.field("timeText")

private val forwardedFromLabel = method("forwardedFromLabel") {
    params(MESSAGE_OBJECT)
    returns(Type.String)
    accesses { owner(R_STRING); name("ForwardedFrom") }
}

// Fills the icons, labels and option ids of a message's menu.
private val messageMenuOptions = method("messageMenuOptions") {
    inClass(chatActivity)
    params(MESSAGE_OBJECT, Type.ArrayList, Type.ArrayList, Type.ArrayList)
    returns(Type.Void)
    accesses { owner(R_STRING); name("Reply") }
}

private val processSelectedOption = method("processSelectedOption") {
    inClass(chatActivity)
    params(Type.Int)
    calls { owner("org.telegram.messenger.MediaController"); name("saveFilesFromMessages") }
}

private val selectedMessage = processSelectedOption
    .point { field { owner(MESSAGE_OBJECT); name("qualityToSave") } }
    .previous { field { type(MESSAGE_OBJECT) } }
    .field("selectedMessage")
