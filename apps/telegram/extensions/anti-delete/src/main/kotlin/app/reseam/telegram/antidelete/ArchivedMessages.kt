// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ArchivedMessages")

package app.reseam.telegram.antidelete

import app.reseam.bundle.Logger
import app.reseam.bundle.useReleasing
import app.reseam.runtime.Export
import org.telegram.tgnet.NativeByteBuffer
import org.telegram.tgnet.TLRPC

// MessagesController.LOAD_* passed to processLoadedMessages.
private const val LOAD_BACKWARD = 0
private const val LOAD_FORWARD = 1
private const val LOAD_AROUND_DATE = 4

// Telegram closes page holes using its first and last message IDs; archived messages must
// stay inside that range and be inserted by ID so the page's ends stay put.
@Export
fun injectDeleted(
    res: TLRPC.messages_Messages?,
    dialogId: Long,
    maxId: Int,
    loadType: Int,
) {
    runCatching {
        val db = archiveDatabase ?: return
        val messages = res?.messages ?: return
        if (!recoversChat(dialogId)) return
        val midToIndex =
            messages
                .asSequence()
                .withIndex()
                .filter { it.value != null && it.value.id > 0 }
                .associate { it.value.id to it.index }
        if (midToIndex.isEmpty()) return
        val newest = midToIndex.keys.max()
        val from = if (loadType == LOAD_FORWARD) maxId + 1 else midToIndex.keys.min()
        val to =
            when (loadType) {
                LOAD_BACKWARD -> if (maxId == 0) Int.MAX_VALUE else maxId - 1
                LOAD_FORWARD -> newest
                else -> if (maxId == 0 && loadType != LOAD_AROUND_DATE) Int.MAX_VALUE else newest
            }
        val added = ArrayList<TLRPC.Message>()
        db.rawQuery(
                "SELECT mid, data FROM deleted WHERE uid = ? AND mid BETWEEN ? AND ?",
                arrayOf(dialogId.toString(), from.toString(), to.toString()),
            )
            .use { cursor ->
                while (cursor.moveToNext()) {
                    val bytes = cursor.getBlob(1)
                    if (bytes == null || bytes.isEmpty()) continue
                    val message = deserialize(bytes) ?: continue
                    message.message = markedText(message.message)
                    val existingIndex = midToIndex[cursor.getInt(0)]
                    if (existingIndex != null) messages[existingIndex] = message
                    else added.add(message)
                }
            }
        added.forEach { messages.add(indexById(messages, it.id), it) }
    }
        .onFailure { Logger.warn(it) { "injectDeleted failed" } }
}

// Pages are newest first; negative IDs belong to local unsent messages and stay where they are.
private fun indexById(messages: ArrayList<TLRPC.Message>, mid: Int): Int =
    messages.indexOfFirst { it != null && it.id > 0 && it.id < mid }.takeIf { it >= 0 }
        ?: messages.size

private fun deserialize(bytes: ByteArray): TLRPC.Message? {
    val buffer = runCatching { NativeByteBuffer(bytes.size) }.getOrNull() ?: return null
    return buffer.useReleasing(NativeByteBuffer::reuse) { data ->
        runCatching {
            data.writeBytes(bytes)
            data.position(0)
            TLRPC.Message.TLdeserialize(data, data.readInt32(false), false)
        }
            .getOrNull()
    }
}
