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

public final class FollowsYouIndicator {
    private static final String LABEL = "Follows you";
    private static final String SUFFIX = " \u00b7 " + LABEL;
    private static final String SETTING_KEY = "follow.follows_you_indicator";

    private FollowsYouIndicator() {}

    public static String maybeAppend(String subtitle, Boolean followedBy) {
        if (!StitchSettings.getBoolean(SETTING_KEY, false)) return subtitle;
        if (followedBy == null || !followedBy.booleanValue()) return subtitle;
        if (subtitle == null || subtitle.isEmpty()) return LABEL;
        if (subtitle.endsWith(SUFFIX) || subtitle.equals(LABEL)) return subtitle;
        return subtitle + SUFFIX;
    }
}
