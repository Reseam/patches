// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock.common

interface SegmentRange {
    val start: Long
    val end: Long
}

// Touching intervals and transitive overlaps count, regardless of input order.
fun <T : SegmentRange> segmentSkipEnd(
    segments: List<T>,
    end: Long,
    eligible: (T) -> Boolean,
): Long {
    var target = end
    while (true) {
        val next =
            segments.fold(target) { stop, segment ->
                if (segment.start <= stop && segment.end > stop && eligible(segment)) segment.end
                else stop
            }
        if (next == target) return target
        target = next
    }
}

// Input is ordered by start; overlapping time is counted once and clipped to the video.
fun <T : SegmentRange> removedSegmentDuration(
    segments: List<T>,
    duration: Long,
    included: (T) -> Boolean,
): Long {
    var end = 0L
    return segments.sumOf { segment ->
        if (!included(segment)) 0L
        else {
            val start = maxOf(end, 0, segment.start)
            val stop = minOf(duration, segment.end)
            end = maxOf(end, stop)
            if (stop > start) stop - start else 0L
        }
    }
}
