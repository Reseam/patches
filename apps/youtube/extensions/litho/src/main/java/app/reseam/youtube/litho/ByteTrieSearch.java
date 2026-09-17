// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho;

import java.nio.charset.StandardCharsets;

/** Binary pattern searching using a prefix tree. */
public final class ByteTrieSearch extends TrieSearch<byte[]> {

    private static final class ByteNode extends Node<byte[]> {
        ByteNode() {
            super();
        }

        ByteNode(char nodeCharacterValue) {
            super(nodeCharacterValue);
        }

        @Override
        Node<byte[]> createNode(char nodeCharacterValue) {
            return new ByteNode(nodeCharacterValue);
        }

        @Override
        char charValue(byte[] text, int index) {
            return (char) text[index];
        }

        @Override
        int textLength(byte[] text) {
            return text.length;
        }
    }

    /** Converts Strings to raw UTF-8 bytes, to search for text in binary data. */
    public static byte[][] convertStringsToBytes(String... strings) {
        final int length = strings.length;
        byte[][] replacement = new byte[length][];
        for (int i = 0; i < length; i++) {
            replacement[i] = strings[i].getBytes(StandardCharsets.UTF_8);
        }
        return replacement;
    }

    public ByteTrieSearch() {
        super(new ByteNode());
    }
}
