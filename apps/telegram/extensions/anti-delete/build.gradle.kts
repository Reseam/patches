// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

extra["dexOutputName"] = "telegram-anti-delete.dex"
extra["dexExcludeClasses"] = listOf(
    "org/telegram/tgnet/InputSerializedData.class",
    "org/telegram/tgnet/OutputSerializedData.class",
    "org/telegram/tgnet/AbstractSerializedData.class",
    "org/telegram/tgnet/NativeByteBuffer.class",
    "org/telegram/tgnet/TLObject.class",
    "org/telegram/tgnet/TLRPC.class",
    "org/telegram/tgnet/TLRPC\$Message.class",
    "org/telegram/tgnet/TLRPC\$messages_Messages.class",
    "org/telegram/SQLite/SQLiteException.class",
    "org/telegram/SQLite/SQLiteCursor.class",
    "org/telegram/SQLite/SQLiteDatabase.class",
    "org/telegram/messenger/MessagesStorage.class",
    "org/telegram/messenger/MessageObject.class",
    "org/telegram/tgnet/TLRPC\$User.class",
    "androidx/recyclerview/widget/RecyclerView.class",
    "androidx/recyclerview/widget/RecyclerView\$Adapter.class",
).joinToString(",")

apply(from = rootDir.resolve("android-extension-module.gradle.kts"))

dependencies {
}
