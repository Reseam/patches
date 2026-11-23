// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("RestrictedForwards")

package app.reseam.telegram.restricted

import android.text.format.Formatter.formatShortFileSize
import app.reseam.bundle.ProgressNotifications
import app.reseam.runtime.Export
import app.reseam.telegram.settings.TelegramStrings
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessageSuggestionParams
import org.telegram.messenger.MessagesController
import org.telegram.messenger.SendMessagesHelper
import org.telegram.tgnet.TLRPC

/**
 * Sends messages from a chat that blocks forwarding as new uploads, after loading any media that
 * is not on the phone yet, with a notification showing that loading. Returns true when it took over
 * the forward.
 */
@Export
fun forward(
    messages: ArrayList<*>?,
    peer: Long,
    payStars: Long,
    monoForumPeerId: Long,
    suggestionParams: MessageSuggestionParams?,
): Boolean {
    val items = messages.orEmpty().filterIsInstance<MessageObject>()
    val first = items.firstOrNull() ?: return false
    if (!first.isProtected) return false
    val helper = SendMessagesHelper.getInstance(first.currentAccount)
    val notification = notifications.newId()
    whenLocal(items, { loaded, total -> showPreparing(notification, loaded, total) }) { ready ->
        notifications.dismiss(notification)
        ready.forEach { helper.processForwardFromMyName(it, peer, payStars, monoForumPeerId, suggestionParams) }
    }
    return true
}

/**
 * Points a re-sent protected message at its local file. Without a path the send references the
 * server's media, which the server rejects for such messages; new media objects make it upload.
 */
@Export
fun uploadLocalCopy(params: SendMessagesHelper.SendMessageParams?) {
    params ?: return
    if (!params.path.isNullOrEmpty() || params.photo == null && params.document == null) return
    val message = params.parentObject as? MessageObject ?: return
    if (!message.isProtected) return
    val file = message.localFile ?: return
    params.photo = params.photo?.uploadCopy()
    params.document = params.document?.uploadCopy()
    params.path = file.absolutePath
}

private val notifications by lazy {
    ProgressNotifications(ApplicationLoader.applicationContext, "reseam_forwards", TelegramStrings.restrictedForwardChannel)
}

private fun showPreparing(notification: Int, loaded: Long, total: Long) {
    val context = ApplicationLoader.applicationContext
    notifications.progress(
        notification,
        TelegramStrings.restrictedForwardPreparing,
        TelegramStrings.restrictedForwardProgress(formatShortFileSize(context, loaded), formatShortFileSize(context, total)),
        loaded.toFloat() / total,
    )
}

private val MessageObject.isProtected: Boolean
    get() = messageOwner.noforwards ||
        dialogId < 0 && MessagesController.getInstance(currentAccount).getChat(-dialogId)?.noforwards == true

// What Telegram fills in for media it is about to upload: no id, access hash or data center.
private fun TLRPC.TL_photo.uploadCopy() = TLRPC.TL_photo().also {
    it.file_reference = ByteArray(0)
    it.date = date
    it.sizes = sizes
    it.video_sizes = video_sizes
    it.has_stickers = has_stickers
}

private fun TLRPC.TL_document.uploadCopy() = TLRPC.TL_document().also {
    it.file_reference = ByteArray(0)
    it.date = date
    it.mime_type = mime_type
    it.size = size
    it.thumbs = thumbs
    it.video_thumbs = video_thumbs
    it.attributes = attributes
    it.file_name = file_name
}
