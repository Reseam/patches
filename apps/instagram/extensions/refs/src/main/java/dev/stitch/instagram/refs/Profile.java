/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.instagram.refs;

/**
 * Bridges to Instagram's profile view-binder / UserDetailViewModel. Bodies are
 * rewritten at patch time by {@code ProfileRefs.kt}.
 */
public final class Profile {
    private Profile() {}

    public static Object viewModel(Object binder) { return null; }

    public static Object relatedDetails(Object binder) { return null; }

    public static boolean isSelf(Object relatedDetails) { return false; }

    public static Object userFromViewModel(Object viewModel) { return null; }
}
