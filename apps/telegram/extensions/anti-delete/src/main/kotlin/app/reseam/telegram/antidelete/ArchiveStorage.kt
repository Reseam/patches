// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ArchiveStorage")

package app.reseam.telegram.antidelete

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.reseam.bundle.Logger
import app.reseam.bundle.useReleasing
import app.reseam.runtime.Export
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.telegram.SQLite.SQLiteCursor
import org.telegram.messenger.MessagesStorage
import org.telegram.tgnet.NativeByteBuffer

@Volatile
internal var archiveDatabase: SQLiteDatabase? = null
    private set

private fun backupArchive(ctx: Context) {
    runCatching {
        val source = ctx.getDatabasePath("reseam_antidelete.db")
        val directory = ctx.getExternalFilesDir(null) ?: return
        if (!source.exists()) return
        val destination = File(directory, "reseam_antidelete_backup.db")
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                input.channel.transferTo(0, input.channel.size(), output.channel)
            }
        }
    }
        .onFailure { Logger.warn(it) { "backup failed" } }
}

@Export
fun init(ctx: Context?) {
    if (ctx == null || archiveDatabase != null) return
    runCatching {
        val app = ctx.applicationContext ?: ctx
        val db = app.openOrCreateDatabase("reseam_antidelete.db", 0, null)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS deleted (" +
                "uid INTEGER NOT NULL, mid INTEGER NOT NULL, " +
                "data BLOB NOT NULL, date INTEGER, PRIMARY KEY(uid, mid))"
        )
        loadRecoveredChats(db)
        archiveDatabase = db
        backupArchive(app)
    }
        .onFailure { Logger.warn(it) { "init failed" } }
}

@Export
fun onMarkDeleted(storage: MessagesStorage?, dialogId: Long, messages: ArrayList<*>?) {
    runCatching {
        val db = archiveDatabase ?: return
        if (storage == null || messages.isNullOrEmpty()) return
        val ids =
            messages
                .asSequence()
                .filterIsInstance<Int>()
                .filterNot { localDeleteMids.remove(it) }
                .joinToString(",")
        if (ids.isEmpty()) return
        val telegramDb = storage.database ?: return
        val where = if (dialogId != 0L) "uid = $dialogId AND mid IN ($ids)" else "mid IN ($ids)"
        telegramDb
            .queryFinalized("SELECT uid, mid, data, date FROM messages_v2 WHERE $where")
            .useReleasing(SQLiteCursor::dispose) { cursor ->
                while (cursor.next()) {
                    val values = cursor.archiveValues() ?: continue
                    db.insertWithOnConflict("deleted", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
    }
        .onFailure { Logger.warn(it) { "onMarkDeleted failed" } }
}

private fun SQLiteCursor.archiveValues(): ContentValues? {
    val data = byteBufferValue(2) ?: return null
    val bytes =
        data.useReleasing(NativeByteBuffer::reuse) { buffer ->
            ByteArray(buffer.limit()).also {
                buffer.position(0)
                buffer.readBytes(it, false)
            }
        }
    if (bytes.isEmpty()) return null
    val uid = longValue(0)
    if (!recoversChat(uid)) return null
    return ContentValues(4).apply {
        put("uid", uid)
        put("mid", intValue(1))
        put("data", bytes)
        put("date", intValue(3))
    }
}
