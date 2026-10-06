// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.instagram.feed;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.reseam.runtime.settings.ReseamSettings;

/** Feed-unit JSON keys hidden by a Reseam toggle; each match checks the toggle, so changes apply to the next feed load. */
public final class FeedUnits {
    private static final class Toggle {
        final String key;
        final boolean fallback;

        Toggle(String key, boolean fallback) {
            this.key = key;
            this.fallback = fallback;
        }
    }

    private static final Map<String, Toggle> HIDDEN = new ConcurrentHashMap<>();

    private FeedUnits() {}

    public static void hide(String unitKey, String toggleKey, boolean toggleDefault) {
        HIDDEN.put(unitKey, new Toggle(toggleKey, toggleDefault));
    }

    public static boolean matches(String field, Object unitKey) {
        if (!field.equals(unitKey)) return false;
        Toggle toggle = HIDDEN.get(field);
        return toggle == null || !ReseamSettings.getBoolean(toggle.key, toggle.fallback);
    }
}
