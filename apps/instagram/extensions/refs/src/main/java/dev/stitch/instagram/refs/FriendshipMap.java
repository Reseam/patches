/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.instagram.refs;

import java.util.Map;

/**
 * Bridge to the friendship-status enum → readable-token map. Body is
 * rewritten at patch time by {@code FriendshipRefs.kt}.
 */
public final class FriendshipMap {
    private FriendshipMap() {}

    public static Map<?, ?> mappings(Object status) { return null; }
}
