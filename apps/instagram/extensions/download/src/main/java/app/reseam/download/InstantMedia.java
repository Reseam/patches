// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.instagram.download;

/** Patched to read the media field on Instagram's current Instants item. */
public final class InstantMedia {
    private InstantMedia() {}

    public static Object media(Object item) {
        return null;
    }
}
