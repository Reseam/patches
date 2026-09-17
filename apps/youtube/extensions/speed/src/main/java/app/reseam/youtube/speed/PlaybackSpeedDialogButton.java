// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.speed;

import android.view.View;

import java.text.DecimalFormat;

import app.reseam.youtube.controls.PlayerControlButton;
import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.video.VideoInformation;

/** A bottom-player button showing the current speed; it opens the speed sheet and resets on long press. */
public final class PlaybackSpeedDialogButton {
    private static final DecimalFormat speedFormatter = new DecimalFormat();
    private static PlayerControlButton button;

    static {
        speedFormatter.setMinimumFractionDigits(1);
        speedFormatter.setMaximumFractionDigits(2);
    }

    private PlaybackSpeedDialogButton() {}

    public static void initialize(View controls) {
        try {
            button = new PlayerControlButton(controls,
                    "reseam_playback_speed_button_container",
                    "reseam_playback_speed_button",
                    "reseam_playback_speed_button_text",
                    () -> Settings.getBoolean("playback_speed_dialog_button", false),
                    view -> {
                        if (Settings.getBoolean("restore_old_playback_speed_menu", false)) {
                            CustomPlaybackSpeed.showOldPlaybackSpeedMenu();
                        } else {
                            CustomPlaybackSpeed.showModernDialog(view.getContext());
                        }
                    },
                    view -> {
                        // Resets to the remembered default, or to 1.0x when already there, unset, or not remembering.
                        float defaultSpeed = RememberPlaybackSpeed.defaultSpeed();
                        float speed = Settings.getBoolean("remember_playback_speed", false) && defaultSpeed > 0
                                && VideoInformation.getPlaybackSpeed() != defaultSpeed ? defaultSpeed : 1.0f;
                        VideoInformation.overridePlaybackSpeed(speed);
                        return true;
                    });
            updateText(VideoInformation.getPlaybackSpeed());
            Logger.debug(() -> "Playback speed dialog button initialized");
        } catch (Exception exception) {
            Logger.error(() -> "Playback speed dialog button initialization failure: " + exception);
        }
    }

    /** Injection point, when the playing speed changes. */
    public static void videoSpeedChanged(float speed) {
        updateText(speed);
    }

    private static synchronized void updateText(float speed) {
        if (button != null) button.setTextOverlay(speedFormatter.format(speed));
    }
}
