// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.reddit.settings;

import com.reddit.settings.usersettings.UserSettingsSection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SettingsRows {
    // App icons and the subscriber's own Premium row live in the Premium section but are not upsells.
    private static final Set<String> KEPT = new HashSet<>(Arrays.asList("key_pref_alt_icons", "key_pref_premium"));

    private SettingsRows() {}

    public static Set<Object> withoutUpsells(Set<?> items) {
        Set<Object> result = new LinkedHashSet<>();
        for (Object item : items) {
            UserSettingsSection section = section(item);
            boolean upsell = section == UserSettingsSection.Premium || section == UserSettingsSection.RedditPro;
            if (!upsell || KEPT.contains(key(item))) result.add(item);
        }
        return result;
    }

    // Reddit's settings item interface is R8-named, so the patch emits these calls.
    static UserSettingsSection section(Object item) {
        throw new UnsupportedOperationException("implemented by the hide premium settings patch");
    }

    static String key(Object item) {
        throw new UnsupportedOperationException("implemented by the hide premium settings patch");
    }
}
