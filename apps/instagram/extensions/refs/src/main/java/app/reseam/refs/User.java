// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.refs;

/**
 * Bridges to Instagram's obfuscated user principal. Bodies are rewritten at
 * patch time by {@code UserRefs.kt}.
 *
 * <p>{@link #fromMedia(Object)} walks the chain
 * {@code media -> dict -> owner wrapper -> user principal}; the other methods
 * accept the resolved user principal.
 */
public final class User {
    private User() {}

    public static Object fromMedia(Object media) { return null; }

    public static String username(Object user) { return null; }
}
