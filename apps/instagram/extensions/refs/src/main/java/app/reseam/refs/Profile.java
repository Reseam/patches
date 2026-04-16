// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.refs;

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
