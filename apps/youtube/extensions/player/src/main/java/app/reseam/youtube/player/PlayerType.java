// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.player;

import java.util.HashMap;
import java.util.Map;

import app.reseam.youtube.core.Logger;

/** What the regular player is doing. Mirrors the app's own player type enum, which drives it. */
public enum PlayerType {
    /** No video, or a Short is playing. */
    NONE,
    /** A Short is playing over a regular video that was opened first and never closed. */
    HIDDEN,
    WATCH_WHILE_MINIMIZED,
    WATCH_WHILE_MAXIMIZED,
    WATCH_WHILE_FULLSCREEN,
    WATCH_WHILE_SLIDING_MAXIMIZED_FULLSCREEN,
    WATCH_WHILE_SLIDING_MINIMIZED_MAXIMIZED,
    /** Sliding to {@link #HIDDEN} because a Short was opened, or swiped away to be closed. */
    WATCH_WHILE_SLIDING_MINIMIZED_DISMISSED,
    /** Home feed video playback. */
    INLINE_MINIMAL,
    VIRTUAL_REALITY_FULLSCREEN,
    WATCH_WHILE_PICTURE_IN_PICTURE;

    private static final Map<String, PlayerType> BY_NAME = new HashMap<>();

    static {
        for (PlayerType type : values()) BY_NAME.put(type.name(), type);
    }

    /** Notified after {@link #current} changes. */
    public static final Event<PlayerType> onChange = new Event<>();

    private static volatile PlayerType current = NONE;

    /** Injection point, from the player overlays layout. */
    public static void set(Enum<?> playerType) {
        if (playerType == null) return;

        PlayerType type = BY_NAME.get(playerType.name());
        if (type == null) {
            Logger.error(() -> "Unknown player type: " + playerType.name());
            return;
        }
        if (type == current) return;

        current = type;
        Logger.debug(() -> "Player type: " + type);
        onChange.fire(type);
    }

    public static PlayerType current() {
        return current;
    }

    /** Nothing is playing, or a Short is. */
    public boolean isNoneOrHidden() {
        return this == NONE || this == HIDDEN;
    }

    public boolean isNoneHiddenOrMinimized() {
        return isNoneHiddenOrSlidingMinimized() || this == WATCH_WHILE_MINIMIZED;
    }

    public boolean isNoneHiddenOrSlidingMinimized() {
        return isNoneOrHidden() || this == WATCH_WHILE_SLIDING_MINIMIZED_DISMISSED;
    }

    public boolean isMaximizedOrFullscreen() {
        return this == WATCH_WHILE_MAXIMIZED || this == WATCH_WHILE_FULLSCREEN;
    }
}
