/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.instagram.refs;

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
