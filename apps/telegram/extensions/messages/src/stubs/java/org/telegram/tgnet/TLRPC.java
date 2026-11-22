// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.telegram.tgnet;

public class TLRPC {
    public static class Message {
        public MessageFwdHeader fwd_from;
    }
    public static class MessageFwdHeader {
        public int date;
    }
}
