// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.controls;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import app.reseam.youtube.core.YouTubeContext;

/** Sizes and theme colors for the dialogs the player buttons open. */
public final class Ui {
    private Ui() {}

    public static int dp(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                Resources.getSystem().getDisplayMetrics());
    }

    /** Radii for a rounded rectangle with the same `dp` radius on every corner. */
    public static float[] roundedCorners(float dp) {
        float radius = dp(dp);
        return new float[]{radius, radius, radius, radius, radius, radius, radius, radius};
    }

    /** `percent` of the screen's shorter side, so a dialog keeps its width in landscape. */
    public static int portraitWidth(int percent) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return Math.min(metrics.widthPixels, metrics.heightPixels) * percent / 100;
    }

    /**
     * Whether the context's theme is dark. YouTube applies its own theme choice without setting the
     * configuration's night mode, so the theme's background color decides.
     */
    public static boolean isDarkMode(Context context) {
        TypedValue background = new TypedValue();
        if (!context.getTheme().resolveAttribute(android.R.attr.colorBackground, background, true)
                || background.type < TypedValue.TYPE_FIRST_COLOR_INT
                || background.type > TypedValue.TYPE_LAST_COLOR_INT) {
            return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
        }
        return Color.luminance(background.data) < 0.5f;
    }

    public static int dialogBackgroundColor(Context context) {
        if (!isDarkMode(context)) return appColor(context, "yt_white1", Color.WHITE);
        int dark = appColor(context, "yt_black1", Color.BLACK);
        // A pure black dialog disappears on the AMOLED theme; lift it slightly.
        return dark == Color.BLACK ? 0xFF080808 : dark;
    }

    public static int foregroundColor(Context context) {
        return isDarkMode(context)
                ? appColor(context, "yt_white1", Color.WHITE)
                : appColor(context, "yt_black1", Color.BLACK);
    }

    /** Lightens the color by `darkFactor` in dark mode, darkens it by `lightFactor` otherwise. */
    public static int adjustBrightness(Context context, int color, float lightFactor, float darkFactor) {
        float factor = isDarkMode(context) ? darkFactor : lightFactor;
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        if (factor > 1.0f) {
            float t = 1.0f - 1.0f / factor;
            red = Math.round(red + (255 - red) * t);
            green = Math.round(green + (255 - green) * t);
            blue = Math.round(blue + (255 - blue) * t);
        } else {
            red = Math.round(red * factor);
            green = Math.round(green * factor);
            blue = Math.round(blue * factor);
        }
        return Color.argb(Color.alpha(color), clamp(red), clamp(green), clamp(blue));
    }

    /** YouTube's own fast fade, which its player controls use. */
    public static int fadeInDuration() {
        Context context = YouTubeContext.get();
        return context.getResources().getInteger(
                context.getResources().getIdentifier("fade_duration_fast", "integer", context.getPackageName()));
    }

    private static int appColor(Context context, String name, int fallback) {
        int id = context.getResources().getIdentifier(name, "color", context.getPackageName());
        return id == 0 ? fallback : context.getColor(id);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
