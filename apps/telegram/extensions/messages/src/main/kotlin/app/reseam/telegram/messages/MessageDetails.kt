// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("MessageDetails")

package app.reseam.telegram.messages

import android.content.ClipData
import android.content.ClipboardManager
import android.text.TextUtils
import app.reseam.bundle.showToast
import app.reseam.runtime.Export
import app.reseam.telegram.settings.TelegramSettings
import app.reseam.telegram.settings.TelegramStrings
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessageObject

private const val EDITED_ICON = "✎"

// Telegram's own menu options are small numbers; this one stays clear of them.
private const val COPY_USER_ID = 0x5EA0

/** The time a message shows, with its ID in front when Show message IDs is on. */
@Export
fun timeText(time: CharSequence?, message: MessageObject?): CharSequence? {
    if (time == null || message == null || !TelegramSettings.showMessageIds.value) return time
    return TextUtils.concat("#${message.id} · ", time)
}

/** Whether a message shows its edited mark at all. */
@Export
fun showsEdited(edited: Boolean): Boolean = edited && TelegramSettings.editedMark.value != "hidden"

/** Whether the edit date replaces the time; a mark of your own goes before the time instead. */
@Export
fun showsEditDate(editDate: Boolean): Boolean = editDate && TelegramSettings.editedMark.value == "telegram"

@Export
fun editedLabel(label: String?): String? =
    when (TelegramSettings.editedMark.value) {
        "icon" -> EDITED_ICON
        "custom" -> TelegramSettings.customEditedMark.value
        else -> label
    }

/** The "Forwarded from" label, with the date the message was first sent when Show forward dates is on. */
@Export
fun forwardedLabel(label: String?, message: MessageObject?): String? {
    if (label == null || !TelegramSettings.showForwardDates.value) return label
    val date = message?.messageOwner?.fwd_from?.date?.takeIf { it > 0 } ?: return label
    return "$label · ${LocaleController.formatDateChat(date.toLong())}"
}

/** Adds the sender's ID to a message's menu. */
@Export
fun addUserId(message: MessageObject?, icons: ArrayList<Any>?, labels: ArrayList<Any>?, options: ArrayList<Any>?, icon: Int) {
    if (message == null || icons == null || labels == null || options == null) return
    if (!TelegramSettings.showUserIds.value || !message.isFromUser) return
    icons.add(icon)
    labels.add(TelegramStrings.userId(message.senderId.toString()))
    options.add(COPY_USER_ID)
}

/** Copies the sender's ID when its menu option was picked. */
@Export
fun onOption(option: Int, message: MessageObject?) {
    if (option != COPY_USER_ID || message == null) return
    val context = ApplicationLoader.applicationContext
    context.getSystemService(ClipboardManager::class.java)
        .setPrimaryClip(ClipData.newPlainText(null, message.senderId.toString()))
    context.showToast(TelegramStrings.userIdCopied)
}
