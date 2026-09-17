// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.theme;

import android.graphics.Color;

import com.airbnb.lottie.LottieAnimationView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.core.YouTubeContext;

/** Runtime color transformations shared by the player, feed and Lottie splash. */
public final class SeekbarColor {
    private static final int ORIGINAL = 0xFFFF0000;
    private static final int[] FEED_COLORS = { 0xFFFF0033, 0xFFFF2791 };
    private static final float[] FEED_POSITIONS = { 0.8f, 1.0f };
    private static final int[] HIDDEN_GRADIENT = { 0, 0 };
    private static final float ORIGINAL_BRIGHTNESS;
    private static final boolean CUSTOM_ENABLED;
    private static final boolean HIDE_THUMBNAIL;
    private static final int CUSTOM_COLOR;
    private static final int[] CUSTOM_GRADIENT = new int[2];
    private static final float[] CUSTOM_HSV = new float[3];

    static {
        float[] hsv = new float[3];
        Color.colorToHSV(ORIGINAL, hsv);
        ORIGINAL_BRIGHTNESS = hsv[2];
        CUSTOM_ENABLED = Settings.getBoolean("seekbar_custom_color", false);
        HIDE_THUMBNAIL = Settings.getBoolean("hide_seekbar_thumbnail", false);
        int primary = ORIGINAL;
        int accent = 0xFFFF2791;
        if (CUSTOM_ENABLED) {
            try {
                primary = Color.parseColor(Settings.getString("seekbar_custom_color_primary", "#FFFF0033"));
                accent = Color.parseColor(Settings.getString("seekbar_custom_color_accent", "#FFFF2791"));
            } catch (Throwable ex) {
                Logger.debug(() -> "Invalid custom seekbar color; using defaults");
            }
        }
        CUSTOM_COLOR = primary;
        CUSTOM_GRADIENT[0] = primary;
        CUSTOM_GRADIENT[1] = accent;
        Color.colorToHSV(primary, CUSTOM_HSV);
    }

    private SeekbarColor() {}

    public static void setSplashAnimationLottie(LottieAnimationView view, int resourceId) {
        try {
            String style = Settings.getString("splash_screen_animation_style", "1");
            if (!CUSTOM_ENABLED || "4".equals(style) || "8".equals(style)) {
                view.patch_setAnimation(resourceId);
                return;
            }
            String json = readRawResource(resourceId);
            String key = "\"k\":";
            String replacement = json
                    .replace(key + "[1,0,0.2,1]", key + colorArray(CUSTOM_COLOR))
                    .replace(key + "[1,0.152941176471,0.56862745098,1]", key + colorArray(CUSTOM_GRADIENT[1]));
            view.patch_setAnimation(new ByteArrayInputStream(replacement.getBytes(StandardCharsets.UTF_8)), null);
        } catch (Throwable ex) {
            Logger.debug(() -> "setSplashAnimationLottie failure: " + ex.getClass().getSimpleName());
            try { view.patch_setAnimation(resourceId); } catch (Throwable ignored) {}
        }
    }

    private static String colorArray(int color) {
        return "[" + (Color.red(color) / 255.0) + "," + (Color.green(color) / 255.0) + ","
                + (Color.blue(color) / 255.0) + "," + (Color.alpha(color) / 255.0) + "]";
    }

    private static String readRawResource(int id) throws IOException {
        try (InputStream in = YouTubeContext.get().getResources().openRawResource(id);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    public static boolean showWatchHistoryProgressDrawable(boolean original) {
        return !HIDE_THUMBNAIL && original;
    }

    public static int getLithoColor(int color) {
        if (color != ORIGINAL) return color;
        return HIDE_THUMBNAIL ? 0 : CUSTOM_COLOR;
    }

    public static int[] getPlayerLinearGradient(int[] original, int x0, int y1) {
        if (HIDE_THUMBNAIL && x0 == 0 && y1 == 0) return HIDDEN_GRADIENT;
        return CUSTOM_ENABLED ? CUSTOM_GRADIENT : original;
    }

    public static int[] getLithoLinearGradient(int[] colors, float[] positions) {
        if (CUSTOM_ENABLED || HIDE_THUMBNAIL) {
            if (Arrays.equals(FEED_COLORS, colors) && Arrays.equals(FEED_POSITIONS, positions)) {
                return HIDE_THUMBNAIL ? HIDDEN_GRADIENT : CUSTOM_GRADIENT;
            }
            Logger.debug(() -> "Ignoring gradient colors: " + Arrays.toString(colors)
                    + " positions: " + Arrays.toString(positions));
        }
        return colors;
    }

    public static int getVideoPlayerSeekbarClickedColor(int color) {
        return CUSTOM_ENABLED && color == ORIGINAL ? CUSTOM_COLOR : color;
    }

    public static int getVideoPlayerSeekbarColor(int original) {
        return CUSTOM_ENABLED ? transformedColor(original) : original;
    }

    private static int transformedColor(int original) {
        try {
            int alphaDifference = Color.alpha(original) - Color.alpha(ORIGINAL);
            float[] hsv = new float[3];
            Color.colorToHSV(original, hsv);
            float brightnessDifference = hsv[2] - ORIGINAL_BRIGHTNESS;
            hsv[0] = CUSTOM_HSV[0];
            hsv[1] = CUSTOM_HSV[1];
            hsv[2] = clamp(CUSTOM_HSV[2] + brightnessDifference, 0, 1);
            int alpha = clamp(Color.alpha(CUSTOM_COLOR) + alphaDifference, 0, 255);
            int replacement = Color.HSVToColor(alpha, hsv);
            Logger.debug(() -> String.format("Seekbar color: #%08X -> #%08X", original, replacement));
            return replacement;
        } catch (Throwable ex) {
            return original;
        }
    }

    private static int clamp(int value, int low, int high) { return Math.max(low, Math.min(high, value)); }
    private static float clamp(float value, float low, float high) { return Math.max(low, Math.min(high, value)); }
}
