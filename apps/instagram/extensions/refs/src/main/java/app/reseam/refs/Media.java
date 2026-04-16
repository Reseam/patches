// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.refs;

import java.util.List;

/**
 * Bridges to Instagram's obfuscated media value class. Every method body is
 * rewritten at patch time by {@code MediaRefs.kt}. Calling any of these in an
 * unpatched APK returns the default value.
 */
public final class Media {
    private Media() {}

    public static String photoUrl(Object media) { return null; }

    public static String videoUrl(Object media) { return null; }

    public static List<?> children(Object media) { return null; }
}
