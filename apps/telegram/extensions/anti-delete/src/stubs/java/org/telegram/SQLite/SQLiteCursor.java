// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

// Compile-only stub mirroring the API surface DeletedArchive uses.
// Real type lives in the Telegram APK; excluded from our dex.
package org.telegram.SQLite;

import org.telegram.tgnet.NativeByteBuffer;

public class SQLiteCursor {
    public boolean next() throws SQLiteException { return false; }
    public int intValue(int columnIndex) throws SQLiteException { return 0; }
    public long longValue(int columnIndex) throws SQLiteException { return 0; }
    public boolean isNull(int columnIndex) throws SQLiteException { return true; }
    public NativeByteBuffer byteBufferValue(int columnIndex) throws SQLiteException { return null; }
    public void dispose() {}
}
