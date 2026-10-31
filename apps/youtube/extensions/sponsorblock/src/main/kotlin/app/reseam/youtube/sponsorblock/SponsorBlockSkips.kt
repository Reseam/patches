// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import android.os.SystemClock
import app.reseam.bundle.Logger
import app.reseam.bundle.ReseamPatches
import app.reseam.youtube.core.debug
import app.reseam.youtube.core.showToast
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.video.VideoInformation
import java.util.Locale

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
        SponsorBlockPlayback.sampleAt(target, SponsorBlockPlayback.lastSeekClock)
        skipped.add(segment.uuid)
        val saved = (target - from).coerceAtLeast(0)
        segments
            .asSequence()
            .filter { it.end > from && it.end <= target && it.isAutomatic }
            .mapTo(skipped) { it.uuid }
        if (counted.add(segment.uuid) && VideoSettings.sbTrackSkipCount.value)
            recordSkip(segment, saved)
        if (VideoSettings.sbToastOnSkip.value) {
            showToast(
                YouTubeStrings.sponsorblockSkipped(
                    segment.category.title.lowercase(Locale.getDefault())
                )
            )
        }
        offerUndo(segment, from, target)
        sponsorBlockMain.removeCallbacks(SponsorBlockPlayback.tickTask)
        sponsorBlockMain.postDelayed(SponsorBlockPlayback.tickTask, 1100)
    }

    private fun canExtendSkip(segment: Segment): Boolean =
        when {
            segment.isHighlight || segment.isIgnored -> false
            !segment.isAutomatic || segment === reverted?.segment -> false
            segment.alreadySkippedOnce -> false
            else -> segment.end - segment.start >= SponsorBlockPlayback.minimumDuration
        }

    private fun recordSkip(segment: Segment, saved: Long) {
        val count = storedSponsorBlockLong("you_tube_settings.sb_local_skip_count")
        val milliseconds = storedSponsorBlockLong("you_tube_settings.sb_local_time_saved")
        val settings = ReseamPatches.bundle().settings
        settings.setString("you_tube_settings.sb_local_skip_count", (count + 1).toString())
        settings.setString(
            "you_tube_settings.sb_local_time_saved",
            (milliseconds + saved).toString(),
        )
        executeSponsorBlockRequest {
            try {
                requestSponsorBlock("POST", "/api/viewedVideoSponsorTime", "UUID" to segment.uuid)
            } catch (error: Exception) {
                Logger.debug(error) { "SponsorBlock view count" }
            }
        }
    }

    internal fun undo(segment: Segment, time: Long, skippedTo: Long = segment.end) {
        if (
            segment !in segments || !SponsorBlockPlayback.isActive || !VideoInformation.seekTo(time)
        )
            return
        reverted = Undo(segment, minOf(time, segment.start), maxOf(segment.end, skippedTo))
        SponsorBlockPlayback.sampleAt(time)
    }
}
