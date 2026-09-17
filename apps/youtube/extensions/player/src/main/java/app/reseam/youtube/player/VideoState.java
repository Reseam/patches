// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.player;

import java.util.HashMap;
import java.util.Map;

import app.reseam.youtube.core.Logger;

/**
 * Playback state of the current video. Depending on which hook reads it, it can lag the player by
 * one state change.
 */
public enum VideoState {
    NEW,
    PLAYING,
    PAUSED,
    RECOVERABLE_ERROR,
    UNRECOVERABLE_ERROR,
    ENDED;

    private static final Map<String, VideoState> BY_NAME = new HashMap<>();

    static {
        for (VideoState state : values()) BY_NAME.put(state.name(), state);
    }

    private static volatile VideoState current;

    /** Injection point, from the player controls state. */
    public static void set(Enum<?> videoState) {
        if (videoState == null) return;

        VideoState state = BY_NAME.get(videoState.name());
        if (state == null) {
            Logger.error(() -> "Unknown video state: " + videoState.name());
            return;
        }
        if (state == current) return;

        current = state;
        Logger.debug(() -> "Video state: " + state);
    }

    /** Null until the first video plays. */
    public static VideoState current() {
        return current;
    }
}
