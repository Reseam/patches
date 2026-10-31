// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("RecoveredChats")

package app.reseam.telegram.antidelete

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import app.reseam.runtime.Export
import app.reseam.telegram.settings.TelegramSettings
import app.reseam.telegram.settings.TelegramStrings
import java.util.concurrent.ConcurrentHashMap

private const val ALL = "all"
private val chosen = ConcurrentHashMap.newKeySet<Long>()
private val excluded = ConcurrentHashMap.newKeySet<Long>()
@Volatile private var chatDatabase: SQLiteDatabase? = null

internal fun loadRecoveredChats(db: SQLiteDatabase) {
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS chats (" +
            "mode TEXT NOT NULL, uid INTEGER NOT NULL, PRIMARY KEY(mode, uid))"
    )
    db.rawQuery("SELECT mode, uid FROM chats", null).use { cursor ->
        while (cursor.moveToNext()) chatList(cursor.getString(0)).add(cursor.getLong(1))
    }
    chatDatabase = db
}

internal fun recoversChat(dialogId: Long): Boolean {
    val mode = recoveryMode
    return (mode == ALL) != (dialogId in chatList(mode))
}

@Export
fun menuLabel(dialogId: Long): String =
    if (recoversChat(dialogId)) TelegramStrings.antiDeleteRecoveryOff
    else TelegramStrings.antiDeleteRecoveryOn

@Export
fun toggle(dialogId: Long): String {
    val mode = recoveryMode
    val list = chatList(mode)
    if (list.remove(dialogId)) {
        checkNotNull(chatDatabase) { "Chat recovery has not loaded" }
            .delete("chats", "mode = ? AND uid = ?", arrayOf(mode, dialogId.toString()))
    } else {
        list.add(dialogId)
        val row =
            ContentValues(2).apply {
                put("mode", mode)
                put("uid", dialogId)
            }
        checkNotNull(chatDatabase) { "Chat recovery has not loaded" }
            .insertWithOnConflict("chats", null, row, SQLiteDatabase.CONFLICT_IGNORE)
    }
    return menuLabel(dialogId)
}

// Both lists survive changes to the recovery mode.
private fun chatList(mode: String): MutableSet<Long> = if (mode == ALL) excluded else chosen

private val recoveryMode: String
    get() = TelegramSettings.recoverDeletedChats.value
