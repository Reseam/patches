// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.instagram.follows;

import app.reseam.runtime.settings.ReseamSettings;

public final class FollowsYouIndicator {
    private static final String LABEL = "Follows you";
    private static final String SUFFIX = " \u00b7 " + LABEL;
    private static final String SETTING_KEY = "follow.follows_you_indicator";

    private FollowsYouIndicator() {}

    public static String maybeAppend(String subtitle, Boolean followedBy) {
        if (!ReseamSettings.getBoolean(SETTING_KEY, true)) return subtitle;
        if (followedBy == null || !followedBy.booleanValue()) return subtitle;
        if (subtitle == null || subtitle.isEmpty()) return LABEL;
        if (subtitle.endsWith(SUFFIX) || subtitle.equals(LABEL)) return subtitle;
        return subtitle + SUFFIX;
    }
}
