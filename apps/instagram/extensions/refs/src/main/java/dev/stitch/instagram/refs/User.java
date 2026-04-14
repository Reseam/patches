/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.instagram.refs;

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

    public static String fullName(Object user) { return null; }

    public static Object friendshipStatus(Object user) { return null; }
}
