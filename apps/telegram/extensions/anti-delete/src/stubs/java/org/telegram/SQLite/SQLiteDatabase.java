// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

// Compile-only stub mirroring the API surface DeletedArchive uses.
// Real type lives in the Telegram APK; excluded from our dex.
package org.telegram.SQLite;

public class SQLiteDatabase {
    public SQLiteCursor queryFinalized(String sql, Object... args) throws SQLiteException { return null; }
}
