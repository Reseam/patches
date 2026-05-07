// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

public class SendMessagesHelper {
    public static SendMessagesHelper getInstance(int num) { return null; }
    public void processForwardFromMyName(
            MessageObject messageObject,
            long did,
            long payStars,
            long monoForumPeerId,
            MessageSuggestionParams suggestionParams) {}

    public static class SendMessageParams {
        public String path;
        public Object parentObject;
        public TLRPC.TL_photo photo;
        public TLRPC.TL_document document;
    }
}
