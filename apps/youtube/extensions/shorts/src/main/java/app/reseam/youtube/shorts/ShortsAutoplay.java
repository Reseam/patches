// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.shorts;

import android.app.Activity;
import java.lang.ref.WeakReference;
import app.reseam.youtube.core.Settings;

/** Selects the native Shorts repeat policy. */
public final class ShortsAutoplay {
    private static WeakReference<Activity> mainActivity = new WeakReference<>(null);
    private static Enum<?> advance;
    private static Enum<?> repeat;

    private ShortsAutoplay() {}

    public static void setMainActivity(Activity activity) {
        mainActivity = new WeakReference<>(activity);
    }

    public static Enum<?> changeRepeatBehavior(Enum<?> original) {
        if (original == null) return null;
        if (advance == null || repeat == null) {
            for (Enum<?> value : original.getDeclaringClass().getEnumConstants()) {
                if (value.name().equals("REEL_LOOP_BEHAVIOR_AUTO_ADVANCE")) advance = value;
                if (value.name().equals("REEL_LOOP_BEHAVIOR_REPEAT")) repeat = value;
            }
        }
        Activity activity = mainActivity.get();
        boolean pip = activity != null && activity.isInPictureInPictureMode();
        boolean autoplay = Settings.getBoolean(pip ? "shorts_autoplay_background" : "shorts_autoplay", pip);
        Enum<?> selected = autoplay ? advance : repeat;
        return selected == null ? original : selected;
    }
}
