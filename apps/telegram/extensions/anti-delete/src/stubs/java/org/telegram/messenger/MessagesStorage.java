// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

// Compile-only stub mirroring the API surface DeletedArchive uses.
// Real type lives in the Telegram APK; excluded from our dex.
package org.telegram.messenger;

import org.telegram.SQLite.SQLiteDatabase;

public class MessagesStorage {
    public SQLiteDatabase getDatabase() { return null; }
}
