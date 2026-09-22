// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock;

import java.util.List;
import java.util.function.Predicate;

/** Interval operations shared by skipping and duration display, independent of Android and HTTP. */
final class SegmentTimeline {
    interface Range {
        long start();
        long end();
    }
    private SegmentTimeline() {}

    /** Includes touching intervals, and handles transitive overlap regardless of input order. */
    static <T extends Range> long skipEnd(List<T> segments, long end, Predicate<T> eligible) {
        boolean expanded;
        do {
            expanded = false;
            for (T segment : segments) {
                if (segment.start() <= end && segment.end() > end && eligible.test(segment)) {
                    end = segment.end();
                    expanded = true;
                }
            }
        } while (expanded);
        return end;
    }

    /** Input must be ordered by start. Overlapping time is counted once and clipped to the video. */
    static <T extends Range> long removedDuration(List<T> segments, long duration, Predicate<T> included) {
        long removed = 0, end = 0;
        for (T segment : segments) {
            if (!included.test(segment)) continue;
            long start = Math.max(end, Math.max(0, segment.start()));
            long stop = Math.min(duration, segment.end());
            if (stop > start) removed += stop - start;
            end = Math.max(end, stop);
        }
        return removed;
    }
}
