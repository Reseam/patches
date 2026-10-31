// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("MessageMarkers")

package app.reseam.telegram.antidelete

import android.util.SparseArray
import app.reseam.bundle.Logger
import app.reseam.bundle.mainThreadHandler
import app.reseam.runtime.Export
import org.telegram.messenger.MessageObject

// Used as a sentinel as well as a visible marker, so it must remain stable.
internal const val DELETED_MARKER = " 🗑️"

private val main = mainThreadHandler()

@Export
fun reapplyMarkers(dict: Array<out SparseArray<*>?>?, adapter: Any?, dialogId: Long) =
    reapplyMarkers(dict, adapter, dialogId, 0)

private fun reapplyMarkers(
    dict: Array<out SparseArray<*>?>?,
    adapter: Any?,
    dialogId: Long,
    attempt: Int,
) {
    runCatching {
        val db = archiveDatabase ?: return
        if (!recoversChat(dialogId)) return
        val messages = dict?.firstOrNull()
        if (messages == null || messages.size() == 0) {
            if (attempt < 5)
                main.postDelayed(
                    { reapplyMarkers(dict, adapter, dialogId, attempt + 1) },
                    300,
                )
            return
        }
        val toMark =
            db.rawQuery("SELECT mid FROM deleted WHERE uid = ?", arrayOf(dialogId.toString()))
                .use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            val mid = cursor.getInt(0)
                            val owner =
                                (messages.get(mid) as? MessageObject)?.messageOwner ?: continue
                            if (owner.message?.endsWith(DELETED_MARKER) != true) add(mid)
                        }
                    }
                }
        if (toMark.isNotEmpty()) applyArchivedMarkers(dict, adapter, toMark)
    }
        .onFailure { Logger.warn(it) { "reapplyMarkers failed" } }
}

internal fun markedText(text: String?): String =
    when {
        text == null -> DELETED_MARKER.trim()
        text.endsWith(DELETED_MARKER) -> text
        else -> text + DELETED_MARKER
    }

internal fun applyArchivedMarkers(
    dict: Array<out SparseArray<*>?>?,
    adapter: Any?,
    mids: List<Int>,
) {
    runCatching {
        val messages = dict?.firstOrNull() ?: return
        var changed = false
        for (mid in mids) {
            val message = messages.get(mid) as? MessageObject ?: continue
            val owner = message.messageOwner ?: continue
            if (owner.message?.endsWith(DELETED_MARKER) == true) continue
            owner.message = markedText(owner.message)
            message.messageText = owner.message
            message.generateLayout(null)
            changed = true
        }
        if (changed && adapter != null) notifyDataSetChanged(adapter)
    }
        .onFailure { Logger.warn(it) { "applyMarkers failed" } }
}
