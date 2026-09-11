// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.x.featureswitches;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.reseam.runtime.settings.ReseamSettings;

/** Feature switch values forced by a Reseam toggle; each read checks the toggle, so changes apply live. */
public final class FeatureSwitchOverrides {
    private static final class Entry {
        final Object value;
        final String toggleKey;
        final boolean toggleDefault;

        Entry(Object value, String toggleKey, boolean toggleDefault) {
            this.value = value;
            this.toggleKey = toggleKey;
            this.toggleDefault = toggleDefault;
        }
    }

    private static final Map<String, Entry> OVERRIDES = new ConcurrentHashMap<>();

    private FeatureSwitchOverrides() {}

    public static void disable(String key, String toggleKey, boolean toggleDefault) {
        OVERRIDES.put(key, new Entry(Boolean.FALSE, toggleKey, toggleDefault));
    }

    /** The forced value for {@code key}, or null when the app's own value applies. */
    public static Object lookup(String key) {
        Entry entry = OVERRIDES.get(key);
        if (entry == null || !ReseamSettings.getBoolean(entry.toggleKey, entry.toggleDefault)) return null;
        return entry.value;
    }
}
