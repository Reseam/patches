// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.core;

import app.reseam.runtime.settings.ReseamSettings;

/**
 * Reads the toggles declared by {@code object YouTubeSettings} in the patches. Extension code that
 * branches at runtime goes through here; hooks emitted by the settings DSL call the shared runtime directly.
 */
public final class Settings {
    /** The bundle derives a key from the settings object and property name: {@code YouTubeSettings.debugLogging}. */
    private static final String PREFIX = "you_tube_settings.";

    private Settings() {}

    public static boolean getBoolean(String key, boolean defaultValue) {
        return ReseamSettings.getBoolean(PREFIX + key, defaultValue);
    }

    public static String getString(String key, String defaultValue) {
        return ReseamSettings.getString(PREFIX + key, defaultValue);
    }
}
