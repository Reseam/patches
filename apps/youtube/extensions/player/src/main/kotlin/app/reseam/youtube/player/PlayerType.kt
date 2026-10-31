// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.player

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug

enum class PlayerType {
    // NONE also covers Shorts; HIDDEN covers Shorts over an unclosed regular video.
    NONE,
    HIDDEN,
    WATCH_WHILE_MINIMIZED,
    WATCH_WHILE_MAXIMIZED,
    WATCH_WHILE_FULLSCREEN,
    WATCH_WHILE_SLIDING_MAXIMIZED_FULLSCREEN,
    WATCH_WHILE_SLIDING_MINIMIZED_MAXIMIZED,
    WATCH_WHILE_SLIDING_MINIMIZED_DISMISSED,
    INLINE_MINIMAL,
    VIRTUAL_REALITY_FULLSCREEN,
    WATCH_WHILE_PICTURE_IN_PICTURE;

    val isNoneOrHidden: Boolean
        get() = this == NONE || this == HIDDEN

    val isNoneHiddenOrMinimized: Boolean
        get() = isNoneHiddenOrSlidingMinimized || this == WATCH_WHILE_MINIMIZED

    val isNoneHiddenOrSlidingMinimized: Boolean
        get() = isNoneOrHidden || this == WATCH_WHILE_SLIDING_MINIMIZED_DISMISSED

    val isMaximizedOrFullscreen: Boolean
        get() = this == WATCH_WHILE_MAXIMIZED || this == WATCH_WHILE_FULLSCREEN

    companion object {
        val onChange = Event<PlayerType>()

        @Volatile
        var current: PlayerType = NONE
            private set

        @Export
        @JvmStatic
        fun set(playerType: Enum<*>?) {
            val type = localEnum<PlayerType>(playerType) ?: return
            if (type == current) return
            current = type
            Logger.debug { "Player type: $type" }
            onChange.fire(type)
        }
    }
}
