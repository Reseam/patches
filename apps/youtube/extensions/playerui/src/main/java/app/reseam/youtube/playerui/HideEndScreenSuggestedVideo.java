// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;

public final class HideEndScreenSuggestedVideo {
    private static volatile boolean autoplayEnabled;

    private HideEndScreenSuggestedVideo() {}

    public static void setAutoplayStatus(boolean enabled) {
        autoplayEnabled = enabled;
        Logger.debug(() -> "HideEndScreenSuggestedVideo.autoplay: " + enabled);
    }

    public static boolean hideEndScreenSuggestedVideo() {
        final boolean hide = Settings.getBoolean("hide_end_screen_suggested_video", false) && !autoplayEnabled;
        Logger.debug(() -> "HideEndScreenSuggestedVideo: " + hide + " autoplay=" + autoplayEnabled);
        return hide;
    }

}
