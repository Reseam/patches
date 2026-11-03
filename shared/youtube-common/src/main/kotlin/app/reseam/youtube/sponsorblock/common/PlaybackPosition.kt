// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock.common

import android.os.SystemClock

// The player reports its position about once a second. In between, the position is estimated as
// if playback went on at its speed since the last report.
class PlaybackPosition {
    var time = 0L
        private set

    private var clock = 0L

    fun sample(time: Long, clock: Long = SystemClock.elapsedRealtime()) {
        this.time = time
        this.clock = clock
    }

    // A report older than a second and a half means playback stopped reporting, as it does while
    // paused, so a resumed playback waits for its first report.
    fun estimate(playing: Boolean, speed: Float): Long {
        val elapsed = SystemClock.elapsedRealtime() - clock
        if (!playing || elapsed > 1500) return time
        return time + (elapsed * speed).toLong()
    }
}

/** Real time until playback at [speed] gets from [now] to [target]. */
fun playbackDelay(now: Long, target: Long, speed: Float): Long =
    ((target - now) / maxOf(.1f, speed)).toLong()
