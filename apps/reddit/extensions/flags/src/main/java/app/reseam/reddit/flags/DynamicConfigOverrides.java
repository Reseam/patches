// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.reddit.flags;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DynamicConfigOverrides {
    private static final Map<String, Map<String, String>> parsed = new ConcurrentHashMap<>();

    private DynamicConfigOverrides() {}

    public static Integer intValue(String name, String overrides) {
        String value = lookup(name, overrides);
        return value == null ? null : Integer.valueOf(value);
    }

    public static String stringValue(String name, String overrides) {
        return lookup(name, overrides);
    }

    // Overrides as "name=value;name=value", written in by the patch that owns the toggle.
    private static String lookup(String name, String overrides) {
        return parsed.computeIfAbsent(overrides, DynamicConfigOverrides::parse).get(name);
    }

    private static Map<String, String> parse(String overrides) {
        Map<String, String> values = new HashMap<>();
        for (String entry : overrides.split(";")) {
            int separator = entry.indexOf('=');
            values.put(entry.substring(0, separator), entry.substring(separator + 1));
        }
        return values;
    }
}
