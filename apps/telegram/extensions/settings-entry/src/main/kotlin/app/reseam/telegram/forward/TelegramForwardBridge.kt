// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("TelegramForwardBridge")

package app.reseam.telegram.forward

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import java.lang.reflect.Modifier
import org.telegram.messenger.FileLoader
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessageSuggestionParams
import org.telegram.messenger.MessagesController
import org.telegram.messenger.SendMessagesHelper

/** Re-sends protected messages through Telegram's helper, which uploads them as new ones. */
@Export
fun tryFakeForward(
    messages: ArrayList<*>?,
    peer: Long,
    payStars: Long,
    monoForumPeerId: Long,
    suggestionParams: MessageSuggestionParams?,
): Boolean {
    if (messages.isNullOrEmpty()) return false
    val first = messages[0] as? MessageObject ?: return false
    val owner = first.messageOwner ?: return false
    val perMessage = owner.noforwards
    val chatLevel = first.chatForbidsForwarding
    if (!perMessage && !chatLevel) return false
    val helper = SendMessagesHelper.getInstance(first.currentAccount) ?: return false
    ArrayList(messages).asSequence().filterIsInstance<MessageObject>().forEach { item ->
        helper.processForwardFromMyName(item, peer, payStars, monoForumPeerId, suggestionParams)
    }
    return true
}

@Export
fun fixPathForNoForwards(params: SendMessagesHelper.SendMessageParams?) {
    if (params == null || !params.path.isNullOrEmpty()) return
    if (params.photo == null && params.document == null) return
    val message = params.parentObject as? MessageObject ?: return
    val owner = message.messageOwner ?: return
    if (!owner.noforwards && !message.chatForbidsForwarding) return
    val loader = FileLoader.getInstance(message.currentAccount) ?: return
    val file = loader.getPathToMessage(message.messageOwner)
    if (file == null || !file.exists() || file.length() <= 0) return
    runCatching {
        params.photo?.let { params.photo = cloneAndDetach(it) }
        params.document?.let { params.document = cloneAndDetach(it) }
    }
        .onFailure {
            Logger.warn(it) { "noforwards forward: clone+detach failed; skipping" }
            return
        }
    params.path = file.absolutePath
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> cloneAndDetach(source: T): T {
    val cls = source.javaClass
    val destination = cls.getDeclaredConstructor().apply { isAccessible = true }.newInstance() as T
    cls.fields
        .asSequence()
        .filterNot { Modifier.isStatic(it.modifiers) || Modifier.isFinal(it.modifiers) }
        .forEach { it.set(destination, it.get(source)) }
    // A zero access_hash makes the send dispatch upload the local file (inputMediaUploadedX).
    try {
        cls.getField("access_hash").setLong(destination, 0L)
    } catch (_: NoSuchFieldException) {}
    return destination
}

private val MessageObject.chatForbidsForwarding: Boolean
    get() {
        val id = dialogId
        return id < 0 &&
            MessagesController.getInstance(currentAccount)?.getChat(-id)?.noforwards == true
    }
