// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.telegram.restricted

import app.reseam.bundle.showToast
import app.reseam.telegram.settings.TelegramStrings
import java.io.File
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLoader
import org.telegram.messenger.ImageLocation
import org.telegram.messenger.MessageObject
import org.telegram.messenger.NotificationCenter

private const val PRIORITY_HIGH = 3
private const val CACHE_ENCRYPTED = 2

/** The file Telegram keeps for a message's media, and how to load it. */
private class Attachment(val name: String, val load: () -> Unit)

internal val MessageObject.localFile: File?
    get() = FileLoader.getInstance(currentAccount).getPathToMessage(messageOwner)?.takeIf { it.exists() && it.length() > 0 }

private fun MessageObject.attachment(): Attachment? {
    val loader = FileLoader.getInstance(currentAccount)
    val cacheType = if (shouldEncryptPhotoOrVideo()) CACHE_ENCRYPTED else 0
    document?.let { document ->
        return Attachment(FileLoader.getAttachFileName(document)) { loader.loadFile(document, this, PRIORITY_HIGH, cacheType) }
    }
    val photo = photo ?: return null
    // The size FileLoader.getPathToMessage names for a photo.
    val size = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize(true), false, null, true) ?: return null
    return Attachment(FileLoader.getAttachFileName(size)) {
        loader.loadFile(ImageLocation.getForObject(size, photo), this, "jpg", PRIORITY_HIGH, cacheType)
    }
}

/**
 * Loads the media of [messages] that is not on the phone yet, then calls [then] on the main thread
 * with every message whose media could be loaded, in their order.
 */
internal fun whenLocal(messages: List<MessageObject>, then: (List<MessageObject>) -> Unit) {
    val missing = messages.filter { it.localFile == null }.mapNotNull { message -> message.attachment()?.let { message to it } }
    if (missing.isEmpty()) return then(messages)
    val waiting = missing.groupBy({ it.second.name }, { it.first }).toMutableMap()
    val failed = mutableSetOf<MessageObject>()
    val center = NotificationCenter.getInstance(messages.first().currentAccount)
    val observer = object : NotificationCenter.NotificationCenterDelegate {
        override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
            val loaded = waiting.remove(args.firstOrNull() as? String) ?: return
            if (id == NotificationCenter.fileLoadFailed) failed += loaded
            if (waiting.isNotEmpty()) return
            center.removeObserver(this, NotificationCenter.fileLoaded)
            center.removeObserver(this, NotificationCenter.fileLoadFailed)
            if (failed.isNotEmpty()) ApplicationLoader.applicationContext.showToast(TelegramStrings.restrictedMediaNotLoaded)
            then(messages - failed)
        }
    }
    center.addObserver(observer, NotificationCenter.fileLoaded)
    center.addObserver(observer, NotificationCenter.fileLoadFailed)
    missing.forEach { it.second.load() }
}
