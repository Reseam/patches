// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.quality;

import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.video.VideoInformation;

/** Applies and stores the selected quality without depending on YouTube's preference UI. */
public final class RememberVideoQuality {
    private RememberVideoQuality() {}

    public static boolean shouldRememberVideoQuality() {
        return Settings.getBoolean("remember_video_quality", false);
    }

    public static int getDefaultQualityResolution() {
        try {
            return Integer.parseInt(Settings.getString("video_quality_default", "-2"));
        } catch (NumberFormatException exception) {
            return VideoInformation.AUTOMATIC_VIDEO_QUALITY_VALUE;
        }
    }

    public static void newVideoStarted(VideoInformation.PlaybackController controller) {
        int resolution = getDefaultQualityResolution();
        VideoInformation.setDesiredVideoResolution(resolution);
        Logger.debug(() -> "Default video quality applied: " + resolution);
    }

    public static void userChangedQuality(int resolution) {
        if (!shouldRememberVideoQuality()) return;
        ReseamSettings.setString("you_tube_settings.video_quality_default", Integer.toString(resolution));
        Logger.debug(() -> "Remembered video quality: " + resolution);
    }

    public static void userChangedShortsQuality(int index) {
        VideoInformation.VideoQualityInterface[] qualities = VideoInformation.getCurrentQualities();
        if (qualities == null || index < 0 || index >= qualities.length || qualities[index] == null) return;
        userChangedQuality(qualities[index].patch_getResolution());
    }
}
