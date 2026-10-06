// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtubemusic.layout;

import app.reseam.runtime.settings.ReseamSettings;

public final class PlaybackSpeed {
    private PlaybackSpeed() {}

    /** Injection point, for every rate the player is given. An empty choice keeps the app's rate. */
    public static float rate(float original) {
        String chosen = ReseamSettings.getChoice("you_tube_music_settings.playback_speed");
        return chosen.isEmpty() ? original : Float.parseFloat(chosen);
    }
}
