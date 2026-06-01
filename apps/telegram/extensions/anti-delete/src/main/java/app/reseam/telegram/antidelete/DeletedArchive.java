// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.telegram.antidelete;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import androidx.recyclerview.widget.RecyclerView;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

public final class DeletedArchive {
    private static final String TAG = "ReseamAntiDelete";
    private static final String MARKER = " 🗑️";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static volatile android.database.sqlite.SQLiteDatabase store;
    private static final Set<Integer> localDeleteMids = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> archivedMids = ConcurrentHashMap.newKeySet();

    private DeletedArchive() {}

    public static void stripSecureFlag(android.view.WindowManager.LayoutParams params) {
        try {
            if (params != null) params.flags &= ~android.view.WindowManager.LayoutParams.FLAG_SECURE;
        } catch (Throwable ignored) {}
    }

    private static void backupTo(Context ctx) {
        try {
            java.io.File src = ctx.getDatabasePath("reseam_antidelete.db");
            java.io.File dir = ctx.getExternalFilesDir(null);
            if (dir == null || !src.exists()) return;
            java.io.File dst = new java.io.File(dir, "reseam_antidelete_backup.db");
            java.io.FileInputStream in = new java.io.FileInputStream(src);
            java.io.FileOutputStream out = new java.io.FileOutputStream(dst);
            try {
                java.nio.channels.FileChannel inCh = in.getChannel();
                inCh.transferTo(0, inCh.size(), out.getChannel());
            } finally { in.close(); out.close(); }
            Log.i(TAG, "backup written to " + dst.getAbsolutePath() + " (" + dst.length() + " bytes)");
        } catch (Throwable t) {
            Log.w(TAG, "backup failed", t);
        }
    }

    public static void init(Context ctx) {
        if (ctx == null || store != null) return;
        try {
            Context app = ctx.getApplicationContext();
            android.database.sqlite.SQLiteDatabase db =
                    (app != null ? app : ctx).openOrCreateDatabase("reseam_antidelete.db", 0, null);
            db.execSQL("CREATE TABLE IF NOT EXISTS deleted ("
                    + "uid INTEGER NOT NULL, mid INTEGER NOT NULL, "
                    + "data BLOB NOT NULL, date INTEGER, PRIMARY KEY(uid, mid))");
            store = db;
            Cursor c = db.rawQuery("SELECT mid FROM deleted", null);
            try { while (c.moveToNext()) archivedMids.add(c.getInt(0)); }
            finally { c.close(); }
            backupTo(app != null ? app : ctx);
        } catch (Throwable t) {
            Log.w(TAG, "init failed", t);
        }
    }

    @SuppressWarnings("rawtypes")
    public static void markLocalDelete(long dialogId, ArrayList messages) {
        if (messages == null) return;
        for (Object o : messages)
            if (o instanceof Integer) localDeleteMids.add((Integer) o);
    }

    @SuppressWarnings("rawtypes")
    public static void onMarkDeleted(MessagesStorage storage, long dialogId, ArrayList messages) {
        try {
            if (store == null || storage == null || messages == null || messages.isEmpty()) return;
            StringBuilder ids = new StringBuilder();
            for (Object o : messages) {
                if (!(o instanceof Integer)) continue;
                int mid = (Integer) o;
                if (localDeleteMids.remove(mid)) continue;
                if (ids.length() > 0) ids.append(',');
                ids.append(mid);
            }
            if (ids.length() == 0) return;

            org.telegram.SQLite.SQLiteDatabase tdb = storage.getDatabase();
            if (tdb == null) return;

            String where = dialogId != 0
                    ? "uid = " + dialogId + " AND mid IN (" + ids + ")"
                    : "mid IN (" + ids + ")";

            int n = 0;
            SQLiteCursor cursor = tdb.queryFinalized(
                    "SELECT uid, mid, data, date FROM messages_v2 WHERE " + where);
            try {
                while (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(2);
                    if (data == null) continue;
                    byte[] bytes;
                    try {
                        bytes = new byte[data.limit()];
                        data.position(0);
                        data.readBytes(bytes, false);
                    } finally { data.reuse(); }
                    if (bytes.length == 0) continue;

                    int mid = cursor.intValue(1);
                    ContentValues cv = new ContentValues(4);
                    cv.put("uid", cursor.longValue(0));
                    cv.put("mid", mid);
                    cv.put("data", bytes);
                    cv.put("date", cursor.intValue(3));
                    long row = store.insertWithOnConflict("deleted", null, cv,
                            android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE);
                    if (row >= 0) {
                        archivedMids.add(mid);
                        n++;
                    }
                }
            } finally { cursor.dispose(); }
            if (n > 0) Log.i(TAG, "archived " + n + " message(s)");
        } catch (Throwable t) {
            Log.w(TAG, "onMarkDeleted failed", t);
        }
    }

    @SuppressWarnings("rawtypes")
    public static void filterDeletedMessages(
            ArrayList list, SparseArray[] dict, Object adapter) {
        if (list == null || list.isEmpty()) return;

        ArrayList<Integer> kept = new ArrayList<>();
        Iterator it = list.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (!(o instanceof Integer)) continue;
            if (!localDeleteMids.contains((Integer) o)) {
                kept.add((Integer) o);
                it.remove();
            }
        }
        if (!kept.isEmpty()) MAIN.post(() -> applyMarkers(dict, adapter, kept));
    }

    public static void reapplyMarkers(SparseArray[] dict, Object adapter) {
        reapplyMarkers(dict, adapter, 0);
    }

    private static void reapplyMarkers(SparseArray[] dict, Object adapter, int attempt) {
        try {
            if (archivedMids.isEmpty()) return;
            if (dict == null || dict.length == 0 || dict[0] == null || dict[0].size() == 0) {
                if (attempt < 5)
                    MAIN.postDelayed(() -> reapplyMarkers(dict, adapter, attempt + 1), 300);
                return;
            }

            ArrayList<Integer> toMark = new ArrayList<>();
            for (int mid : archivedMids) {
                Object obj = dict[0].get(mid);
                if (!(obj instanceof MessageObject)) continue;
                MessageObject mo = (MessageObject) obj;
                if (mo.messageOwner == null) continue;
                if (mo.messageOwner.message != null && mo.messageOwner.message.endsWith(MARKER)) continue;
                toMark.add(mid);
            }
            if (!toMark.isEmpty()) applyMarkers(dict, adapter, toMark);
        } catch (Throwable t) {
            Log.w(TAG, "reapplyMarkers failed", t);
        }
    }

    public static void injectDeleted(TLRPC.messages_Messages res, long dialogId) {
        try {
            if (store == null || res == null || res.messages == null) return;

            java.util.HashMap<Integer, Integer> midToIndex = new java.util.HashMap<>();
            for (int i = 0; i < res.messages.size(); i++) {
                TLRPC.Message m = res.messages.get(i);
                if (m != null) midToIndex.put(m.id, i);
            }

            Cursor c = store.rawQuery(
                    "SELECT mid, data FROM deleted WHERE uid = ?",
                    new String[]{String.valueOf(dialogId)});
            int n = 0;
            try {
                while (c.moveToNext()) {
                    int mid = c.getInt(0);
                    byte[] bytes = c.getBlob(1);
                    if (bytes == null || bytes.length == 0) continue;

                    TLRPC.Message m = deserialize(bytes);
                    if (m == null) continue;
                    if (m.message == null) m.message = MARKER.trim();
                    else if (!m.message.endsWith(MARKER)) m.message = m.message + MARKER;
                    Integer existingIndex = midToIndex.get(mid);
                    if (existingIndex != null) res.messages.set(existingIndex, m);
                    else res.messages.add(m);
                    n++;
                }
            } finally { c.close(); }
            if (n > 0) {
                java.util.Collections.sort(res.messages, (a, b) -> {
                    if (a.date != b.date) return Integer.compare(b.date, a.date);
                    return Integer.compare(b.id, a.id);
                });
                Log.i(TAG, "injected " + n + " archived message(s) for dialog " + dialogId);
            }
        } catch (Throwable t) {
            Log.w(TAG, "injectDeleted failed", t);
        }
    }

    private static TLRPC.Message deserialize(byte[] bytes) {
        NativeByteBuffer buf = null;
        try {
            buf = new NativeByteBuffer(bytes.length);
            buf.writeBytes(bytes);
            buf.position(0);
            return TLRPC.Message.TLdeserialize(buf, buf.readInt32(false), false);
        } catch (Throwable t) {
            return null;
        } finally {
            if (buf != null) buf.reuse();
        }
    }

    private static void applyMarkers(SparseArray[] dict, Object adapter, ArrayList<Integer> mids) {
        try {
            if (dict == null || dict.length == 0 || dict[0] == null) return;

            int marked = 0;
            for (int mid : mids) {
                Object obj = dict[0].get(mid);
                if (!(obj instanceof MessageObject)) continue;
                MessageObject mo = (MessageObject) obj;
                if (mo.messageOwner == null) continue;

                String msg = mo.messageOwner.message;
                if (msg == null) mo.messageOwner.message = MARKER.trim();
                else if (!msg.endsWith(MARKER)) mo.messageOwner.message = msg + MARKER;
                else continue;

                mo.messageText = mo.messageOwner.message;
                mo.generateLayout(null);
                marked++;
            }
            if (marked > 0 && adapter instanceof RecyclerView.Adapter)
                ((RecyclerView.Adapter<?>) adapter).notifyDataSetChanged();
        } catch (Throwable t) {
            Log.w(TAG, "applyMarkers failed", t);
        }
    }
}
