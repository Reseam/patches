// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.theme;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

/** Runtime choices for YouTube's theme and loading screen. */
public final class ThemePatch {
    private static final int[] WHITE_VALUES = { 0xFFFFFFFF, 0xFFF9F9F9, 0xFAFFFFFF };
    private static final int[] DARK_VALUES = {
            0xFF282828, 0xFF212121, 0xFF181818, 0xFF0F0F0F, 0xFA212121,
    };

    private ThemePatch() {}

    public static int getValue(int originalValue) {
        for (int value : DARK_VALUES) if (originalValue == value) return color("yt_black1", originalValue);
        for (int value : WHITE_VALUES) if (originalValue == value) return color("yt_white1", originalValue);
        return originalValue;
    }

    private static int color(String name, int fallback) {
        try {
            int id = app.reseam.youtube.core.YouTubeContext.get().getResources()
                    .getIdentifier(name, "color", app.reseam.youtube.core.YouTubeContext.get().getPackageName());
            return id == 0 ? fallback : app.reseam.youtube.core.YouTubeContext.get().getResources().getColor(id);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static boolean gradientLoadingScreenEnabled(boolean original) {
        return Settings.getBoolean("gradient_loading_screen", false) || original;
    }

    public static boolean showSplashScreen(boolean original) {
        return !"0".equals(Settings.getString("splash_screen_animation_style", "1")) && original;
    }

    /** State 5 is the fallback path into splash creation when the startup predicate is false. */
    public static int splashStartupState(int original) {
        return original == 5 && "0".equals(Settings.getString("splash_screen_animation_style", "1"))
                ? 4 : original;
    }

    public static int getLoadingScreenType(int original) {
        String selected = Settings.getString("splash_screen_animation_style", "1");
        if ("0".equals(selected)) return original;
        try {
            int replacement = Integer.parseInt(selected);
            if (replacement != original) {
                Logger.debug(() -> "Loading screen type: " + original + " -> " + replacement);
            }
            return replacement;
        } catch (NumberFormatException ignored) {
            return original;
        }
    }
}
