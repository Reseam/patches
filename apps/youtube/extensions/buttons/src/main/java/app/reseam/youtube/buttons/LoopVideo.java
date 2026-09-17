// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.buttons;

import android.view.View;

import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.youtube.controls.PlayerControlButton;
import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.video.VideoInformation;

public final class LoopVideo {
    private LoopVideo() {}

    public static void initialize(View root) {
        try {
            new PlayerControlButton(root, "reseam_loop_video_button",
                    () -> Settings.getBoolean("loop_video_button", false),
                    LoopVideo::toggle, null);
            Logger.debug(() -> "Loop video button initialized");
        } catch (Exception exception) {
            Logger.error(() -> "Loop video button initialization failure: " + exception);
        }
    }

    private static void toggle(View ignored) {
        final boolean enabled = !Settings.getBoolean("loop_video", false);
        ReseamSettings.setBoolean("you_tube_settings.loop_video", enabled);
        Logger.debug(() -> "Loop video " + (enabled ? "enabled" : "disabled"));
    }

    public static boolean shouldLoopVideo(Enum<?> status) {
        final boolean loop = status != null && "ENDED".equals(status.name())
                && Settings.getBoolean("loop_video", false);
        if (loop) {
            boolean started = VideoInformation.seekTo(0);
            if (started) Logger.debug(() -> "Looping video");
            return started;
        }
        return loop;
    }
}
