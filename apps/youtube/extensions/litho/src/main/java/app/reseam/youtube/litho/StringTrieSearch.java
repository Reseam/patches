// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.litho;

/** Text pattern searching using a prefix tree. */
public final class StringTrieSearch extends TrieSearch<String> {

    private static final class StringNode extends Node<String> {
        StringNode() {
            super();
        }

        StringNode(char nodeCharacterValue) {
            super(nodeCharacterValue);
        }

        @Override
        Node<String> createNode(char nodeValue) {
            return new StringNode(nodeValue);
        }

        @Override
        char charValue(String text, int index) {
            return text.charAt(index);
        }

        @Override
        int textLength(String text) {
            return text.length();
        }
    }

    public StringTrieSearch() {
        super(new StringNode());
    }
}
