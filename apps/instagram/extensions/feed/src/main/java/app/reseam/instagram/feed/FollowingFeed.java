// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.instagram.feed;

import java.util.HashMap;
import java.util.Map;

public final class FollowingFeed {
    private static final String PAGINATION_SOURCE = "pagination_source";

    private FollowingFeed() {}

    public static Map<Object, Object> following(Map<?, ?> params) {
        Map<Object, Object> result = params == null ? new HashMap<>() : new HashMap<>(params);
        Object source = result.get(PAGINATION_SOURCE);
        if (source == null || "feed_recs".equals(source)) result.put(PAGINATION_SOURCE, "following");
        return result;
    }
}
