// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

public class MessageObject {
    public int currentAccount;
    public TLRPC.Message messageOwner;
    public long getDialogId() { return 0; }
}
