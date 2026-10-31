// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import android.os.SystemClock

internal sealed interface PlaybackDecision {
    data object None : PlaybackDecision

    data class Skip(val segment: Segment) : PlaybackDecision

    data class Offer(val segment: Segment) : PlaybackDecision

    data class WakeAt(val time: Long) : PlaybackDecision
}

internal val Segment.alreadySkippedOnce: Boolean
    get() = category.mode == "skip-once" && uuid in SponsorBlockSkips.skipped

internal fun Segment.playbackDecision(now: Long): PlaybackDecision {
    if (isIgnored) return PlaybackDecision.None
    val belowMinimumDuration = end - start < SponsorBlockPlayback.minimumDuration && !isHighlight
    if (belowMinimumDuration) return PlaybackDecision.None
    val undo = SponsorBlockSkips.reverted
    if (isHighlight) {
        return when {
            now >= start || uuid in SponsorBlockSkips.skipped -> PlaybackDecision.None
            isAutomatic && undo?.segment !== this -> PlaybackDecision.Skip(this)
            category.mode != "seekbar-only" -> PlaybackDecision.Offer(this)
            else -> PlaybackDecision.None
        }
    }
    if (now < start) return PlaybackDecision.WakeAt(start)
    if (now >= end) return PlaybackDecision.None
    val overlapsUndo = undo != null && start < undo.end && end > undo.start
    if (overlapsUndo) return PlaybackDecision.None
    return when {
        isAutomatic && !alreadySkippedOnce -> {
            val seekSettled =
                SystemClock.elapsedRealtime() - SponsorBlockPlayback.lastSeekClock > 1000
            if (seekSettled) PlaybackDecision.Skip(this) else PlaybackDecision.None
        }
        category.mode != "seekbar-only" -> PlaybackDecision.Offer(this)
        else -> PlaybackDecision.None
    }
}
