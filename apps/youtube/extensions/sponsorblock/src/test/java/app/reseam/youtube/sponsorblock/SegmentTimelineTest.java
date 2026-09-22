// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock;

import java.util.List;

/** Standalone JVM regression checks; does not need an Android runtime or network. */
public final class SegmentTimelineTest {
    private record Range(long start, long end) implements SegmentTimeline.Range {}
    private static void equal(long expected, long actual) {
        if (actual != expected) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    public static void main(String[] args) {
        Range sponsor = new Range(10, 20), overlap = new Range(18, 25), touching = new Range(25, 30);
        equal(30, SegmentTimeline.skipEnd(List.of(touching, overlap, sponsor), 20, range -> true));
        equal(20, SegmentTimeline.skipEnd(List.of(sponsor, new Range(21, 25)), 20, range -> true));
        equal(20, SegmentTimeline.skipEnd(List.of(sponsor, overlap, touching), 20, range -> range == sponsor));
        equal(15, SegmentTimeline.removedDuration(List.of(sponsor, overlap), 100, range -> true));
        equal(12, SegmentTimeline.removedDuration(List.of(sponsor, overlap), 22, range -> true));
        equal(10, SegmentTimeline.removedDuration(List.of(sponsor, overlap), 100, range -> range == sponsor));
        equal(0, SegmentTimeline.removedDuration(List.of(new Range(50, 60)), 40, range -> true));
        equal(0, SegmentTimeline.removedDuration(List.of(new Range(15, 15)), 40, range -> true));
        equal(5, SegmentTimeline.removedDuration(List.of(new Range(-5, 5)), 40, range -> true));
        equal(0, SegmentTimeline.removedDuration(List.<Range>of(), 40, range -> true));
        System.out.println("SponsorBlock timeline: 10 regression checks passed");
    }
}
