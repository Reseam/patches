// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

public class MessageObject {
    public CharSequence messageText;
    public TLRPC.Message messageOwner;
    public void generateLayout(TLRPC.User fromUser) {}
}
