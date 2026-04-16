// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.download;

final class StoryOptions {
    private static final String LABEL = "Download";

    private StoryOptions() {}

    static boolean isDownload(CharSequence option) {
        return option != null && LABEL.contentEquals(option);
    }

    static CharSequence[] appendDownload(CharSequence[] items) {
        if (items == null) return new CharSequence[]{LABEL};
        for (CharSequence item : items) {
            if (isDownload(item)) return items;
        }

        CharSequence[] expanded = new CharSequence[items.length + 1];
        System.arraycopy(items, 0, expanded, 0, items.length);
        expanded[items.length] = LABEL;
        return expanded;
    }
}
