// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import android.os.SystemClock
import app.reseam.youtube.sponsorblock.common.Segment
import app.reseam.youtube.sponsorblock.common.isTooShort
import app.reseam.youtube.sponsorblock.common.reportSponsorBlockSkip
import app.reseam.youtube.sponsorblock.common.segmentSkipEnd
import app.reseam.youtube.video.VideoInformation

internal object SponsorBlockSkips {
    private val segments
        get() = SponsorBlockSession.segments

    val skipped = mutableSetOf<String>()
    private val counted = mutableSetOf<String>()

    data class Undo(val segment: Segment, val start: Long, val end: Long)

    var reverted: Undo? = null
        private set

    fun reset() {
        skipped.clear()
        counted.clear()
        reverted = null
    }

    fun expireUndo(now: Long) {
        val undo = reverted ?: return
        if (now < undo.start || now >= undo.end) reverted = null
    }

    internal fun skip(segment: Segment) {
        if (!SponsorBlockPlayback.isActive) return
        val from = SponsorBlockPlayback.estimatedTime
        val end =
            if (segment.isHighlight) segment.start
            else segmentSkipEnd(segments, segment.end, ::canExtendSkip)
        val duration = VideoInformation.videoLength
        val target = if (duration > 0) minOf(end, (duration - 250).coerceAtLeast(0)) else end
        if (target <= from || !VideoInformation.seekTo(target)) return
        SponsorBlockPlayback.lastSeekClock = SystemClock.elapsedRealtime()
        SponsorBlockPlayback.position.sample(target, SponsorBlockPlayback.lastSeekClock)
        skipped.add(segment.uuid)
        val saved = (target - from).coerceAtLeast(0)
        segments
            .asSequence()
            .filter { it.end > from && it.end <= target && it.isAutomatic }
            .mapTo(skipped) { it.uuid }
        reportSponsorBlockSkip(segment, saved, counted.add(segment.uuid))
        offerUndo(segment, from, target)
        sponsorBlockMain.removeCallbacks(SponsorBlockPlayback.tickTask)
        sponsorBlockMain.postDelayed(SponsorBlockPlayback.tickTask, 1100)
    }

    private fun canExtendSkip(segment: Segment): Boolean =
        when {
            segment.isHighlight || segment.isIgnored -> false
            !segment.isAutomatic || segment === reverted?.segment -> false
            segment.alreadySkippedOnce -> false
            else -> !segment.isTooShort
        }

    internal fun undo(segment: Segment, time: Long, skippedTo: Long = segment.end) {
        if (
            segment !in segments || !SponsorBlockPlayback.isActive || !VideoInformation.seekTo(time)
        )
            return
        reverted = Undo(segment, minOf(time, segment.start), maxOf(segment.end, skippedTo))
        SponsorBlockPlayback.position.sample(time)
    }
}
