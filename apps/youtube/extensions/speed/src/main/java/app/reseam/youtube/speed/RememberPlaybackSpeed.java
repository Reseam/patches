// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.speed;

import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.video.VideoInformation;

/** Applies the default speed once per video and persists explicit selections. */
public final class RememberPlaybackSpeed {
    private static volatile boolean newVideoStarted;

    private RememberPlaybackSpeed() {}

    public static void newVideoStarted(VideoInformation.PlaybackController controller) {
        newVideoStarted = true;
    }

    /** Called when YouTube initializes its speed values: after a video loads, and after every selection. */
    public static void applyDefaultPlaybackSpeed() {
        if (!newVideoStarted) return;
        newVideoStarted = false;
        final float speed = defaultSpeed();
        if (speed <= 0.0f) return;
        VideoInformation.overridePlaybackSpeed(speed);
        Logger.debug(() -> "Default playback speed applied: " + speed);
    }

    /** The configured default speed, or 0 when none is set. */
    public static float defaultSpeed() {
        return parse(Settings.getString("playback_speed_default", "0"));
    }

    public static void userSelectedPlaybackSpeed(float speed) {
        if (!Settings.getBoolean("remember_playback_speed", false)) return;
        ReseamSettings.setString("you_tube_settings.playback_speed_default", Float.toString(speed));
        Logger.debug(() -> "Remembered playback speed: " + speed);
    }

    private static float parse(String value) {
        try {
            float speed = Float.parseFloat(value);
            return speed > 0 && speed <= CustomPlaybackSpeed.PLAYBACK_SPEED_MAXIMUM ? speed : 0.0f;
        } catch (Exception exception) {
            return 0.0f;
        }
    }
}
