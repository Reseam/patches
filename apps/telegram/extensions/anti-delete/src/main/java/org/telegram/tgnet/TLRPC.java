// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

// Compile-only stub mirroring the API surface DeletedArchive uses.
// Real types live in the Telegram APK; excluded from our dex.
package org.telegram.tgnet;

import java.util.ArrayList;

public class TLRPC {
    public static class Message extends TLObject {
        public int id;
        public int date;
        public String message;

        public static Message TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            return null;
        }
    }

    public static class messages_Messages extends TLObject {
        public ArrayList<Message> messages = new ArrayList<>();
    }

    public static class User extends TLObject {}
}
