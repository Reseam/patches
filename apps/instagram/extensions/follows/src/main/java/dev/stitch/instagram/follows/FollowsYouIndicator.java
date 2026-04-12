/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.instagram.follows;

import dev.stitch.runtime.settings.StitchSettings;

/**
 * Appends a " . Follows you" suffix to a subtitle string. Wired in by FollowsYouPatch
 * just before every RETURN_OBJECT in the search-result subtitle builder.
 * <p>
 * This naive version tags every subtitle unconditionally. A proper implementation
 * needs access to the row's FriendshipStatus — the subtitle method doesn't receive it
 * directly, so that will require a small helper that walks the caller chain (see
 * WORK_LOG notes). Until then, the patch stays off by default.
 */
public final class FollowsYouIndicator {
    private static final String SUFFIX = " \u00b7 Follows you";
    private static final String SETTING_KEY = "follow.follows_you_indicator";

    private FollowsYouIndicator() {}

    public static String maybeAppend(String subtitle) {
        if (!StitchSettings.getBoolean(SETTING_KEY, false)) return subtitle;
        if (subtitle == null || subtitle.isEmpty()) return subtitle;
        if (subtitle.endsWith(SUFFIX)) return subtitle;
        return subtitle + SUFFIX;
    }
}
