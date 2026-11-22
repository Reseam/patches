// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

public class MessageObject {
    public int currentAccount;
    public TLRPC.Message messageOwner;
    public long getDialogId() { return 0; }
    public TLRPC.Document getDocument() { return null; }
    public TLRPC.Photo getPhoto() { return null; }
    public boolean shouldEncryptPhotoOrVideo() { return false; }
}
