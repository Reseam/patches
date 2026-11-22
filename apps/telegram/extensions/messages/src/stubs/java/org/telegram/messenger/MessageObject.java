// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

public class MessageObject {
    public TLRPC.Message messageOwner;
    public int getId() { return 0; }
    public long getSenderId() { return 0; }
    public boolean isFromUser() { return false; }
}
