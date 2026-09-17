// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.misc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

/**
 * YouTube reads every A/B experiment through a handful of accessors keyed by a numeric flag id.
 * Overriding them from a setting is how a user turns a server-side experiment on or off, which is
 * the only part of ReVanced's "Enable debugging" patch that is not a settings screen.
 */
public final class FeatureFlags {
    private static volatile Map<Long, String> overrides;

    private FeatureFlags() {}

    /** Injection point, from the boolean experiment accessor. */
    public static boolean booleanOverride(long id, boolean original) {
        String value = overrides().get(id);
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) return original;
        boolean override = Boolean.parseBoolean(value);
        Logger.debug(() -> "Feature flag " + id + ": " + original + " -> " + override);
        return override;
    }

    /** Injection point, from the string experiment accessor. */
    public static String stringOverride(long id, String original) {
        String value = overrides().get(id);
        if (value == null) return original;
        Logger.debug(() -> "Feature flag " + id + ": '" + original + "' -> '" + value + "'");
        return value;
    }

    /**
     * Parsed once: these accessors run on every flag read, several thousand times a session.
     */
    private static Map<Long, String> overrides() {
        Map<Long, String> parsed = overrides;
        if (parsed != null) return parsed;
        parsed = parse(Settings.getString("feature_flag_overrides", ""));
        overrides = parsed;
        return parsed;
    }

    private static Map<Long, String> parse(String setting) {
        Map<Long, String> parsed = new HashMap<>();
        for (String entry : setting.split(",")) {
            int separator = entry.indexOf('=');
            if (separator < 0) continue;
            String id = entry.substring(0, separator).trim();
            try {
                parsed.put(Long.parseLong(id), entry.substring(separator + 1).trim());
            } catch (NumberFormatException ex) {
                Logger.error(() -> "Ignoring feature flag override with a non-numeric id: " + id);
            }
        }
        return Collections.unmodifiableMap(parsed);
    }
}
