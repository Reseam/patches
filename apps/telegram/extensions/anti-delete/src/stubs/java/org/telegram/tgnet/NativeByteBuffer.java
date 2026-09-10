// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

// Compile-only stub mirroring the API surface DeletedArchive uses.
// Real type lives in the Telegram APK; excluded from our dex.
package org.telegram.tgnet;

public class NativeByteBuffer extends AbstractSerializedData {
    public NativeByteBuffer(int size) throws Exception {}
    public int position() { return 0; }
    public void position(int position) {}
    public int limit() { return 0; }
    public int readInt32(boolean exception) { return 0; }
    public void readBytes(byte[] b, boolean exception) {}
    public void writeBytes(byte[] b) {}
    public void reuse() {}
}
