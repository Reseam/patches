// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtubemusic.layout;

public final class MusicTheme {
    private static final int PURE_BLACK = 0xFF000000;

    private MusicTheme() {}

    /** Replaced at patch time with the chosen background color. */
    private static int background() {
        return PURE_BLACK;
    }

    /** Injection point, for every color litho paints a background with. */
    public static int paintColor(int original) {
        return original == PURE_BLACK ? background() : original;
    }
}
