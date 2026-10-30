// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("DeletedArchive")

package app.reseam.telegram.antidelete

import android.util.SparseArray
import app.reseam.bundle.postToMainThread
import app.reseam.runtime.Export
import java.util.concurrent.ConcurrentHashMap

internal val localDeleteMids = ConcurrentHashMap.newKeySet<Int>()

// R8 renames RecyclerView, so the anti-delete patch emits this body.
@Export external fun notifyDataSetChanged(adapter: Any): Unit

@Export
fun markLocalDelete(dialogId: Long, messages: ArrayList<*>?) {
    messages?.filterIsInstanceTo(localDeleteMids)
}

// Telegram shares this list across chats; channel deletions concern only that channel's chat.
@Export
fun filterDeletedMessages(
    list: ArrayList<*>?,
    channelId: Long,
    dialogId: Long,
    dict: Array<out SparseArray<*>?>?,
    adapter: Any?,
): ArrayList<*>? {
    if (list.isNullOrEmpty()) return list
    val isOtherChannel = channelId != 0L && channelId != -dialogId
    if (isOtherChannel || !recoversChat(dialogId)) return list
    val (kept, toRemove) = list.partition { it is Int && it !in localDeleteMids }
    val mids = kept.filterIsInstance<Int>()
    if (mids.isNotEmpty()) postToMainThread { applyArchivedMarkers(dict, adapter, mids) }
    return ArrayList(toRemove)
}
