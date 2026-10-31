// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.player

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug

enum class VideoState {
    NEW,
    PLAYING,
    PAUSED,
    RECOVERABLE_ERROR,
    UNRECOVERABLE_ERROR,
    ENDED;

    companion object {
        /** Null until the first video plays; some app hooks lag playback by one state change. */
        @Volatile
        var current: VideoState? = null
            private set

        @Export
        @JvmStatic
        fun set(videoState: Enum<*>?) {
            val state = localEnum<VideoState>(videoState) ?: return
            if (state == current) return
            current = state
            Logger.debug { "Video state: $state" }
        }
    }
}
