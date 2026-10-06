// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtubemusic.core;

import java.util.function.BooleanSupplier;

import app.reseam.runtime.settings.ReseamSettings;

/** Reads the toggles declared by {@code object YouTubeMusicSettings} in the patches. */
public final class MusicSettings {
    private static final String PREFIX = "you_tube_music_settings.";

    private MusicSettings() {}

    public static BooleanSupplier toggle(String key, boolean defaultValue) {
        return () -> ReseamSettings.getBoolean(PREFIX + key, defaultValue);
    }
}
