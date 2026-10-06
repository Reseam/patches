// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.reddit.flags;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import app.reseam.runtime.settings.ReseamSettings;

public final class FlagOverrides {
    private static volatile Overrides overrides;

    private FlagOverrides() {}

    public static boolean isDisabled(String flag) {
        return overrides().disabled.contains(flag);
    }

    public static boolean isForced(String flag) {
        return overrides().forced.contains(flag);
    }

    private static Overrides overrides() {
        Overrides current = overrides;
        if (current != null) return current;
        current = new Overrides();
        for (String group : spec().split(";")) {
            String[] parts = group.split("\\|", -1);
            if (!ReseamSettings.getBoolean(parts[0], Boolean.parseBoolean(parts[1]))) continue;
            addAll(current.disabled, parts[2]);
            addAll(current.forced, parts[3]);
        }
        // Flags are read before settings load; keep the defaults until they have.
        if (ReseamSettings.prefs() != null) overrides = current;
        return current;
    }

    private static void addAll(Set<String> target, String csv) {
        if (!csv.isEmpty()) Collections.addAll(target, csv.split(","));
    }

    // Groups as "settingKey|default|disabled,flags|forced,flags;...", written in by the patch that owns the toggles.
    static String spec() {
        throw new UnsupportedOperationException("implemented by the disable features patch");
    }

    private static final class Overrides {
        final Set<String> disabled = new HashSet<>();
        final Set<String> forced = new HashSet<>();
    }
}
