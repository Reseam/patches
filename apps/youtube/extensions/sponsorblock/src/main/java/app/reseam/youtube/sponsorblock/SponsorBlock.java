// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import app.reseam.runtime.settings.ReseamSettings;
import app.reseam.youtube.core.Logger;
import app.reseam.youtube.core.Settings;
import app.reseam.youtube.controls.PlayerControls;
import app.reseam.youtube.player.PlayerType;
import app.reseam.youtube.player.VideoState;
import app.reseam.youtube.player.ShortsPlayerState;
import app.reseam.youtube.video.VideoInformation;

/** Playback and UI state live on the main thread; fetched segment lists are immutable. */
public final class SponsorBlock {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Map<String, CacheEntry> CACHE = new LinkedHashMap<String, CacheEntry>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> entry) { return size() > 100; }
    };
    private static final Set<String> skipped = new HashSet<>();
    private static final Set<String> counted = new HashSet<>();
    private static WeakReference<SponsorBlockOverlay> overlay = new WeakReference<>(null);
    private static WeakReference<View> player = new WeakReference<>(null);
    private static final View.OnLayoutChangeListener PLAYER_LAYOUT = (view, l, t, r, b, ol, ot, or, ob) -> {
        SponsorBlockOverlay controls = overlay.get();
        if (controls != null) controls.alignTo(view);
    };
    private static volatile List<Segment> segments = Collections.emptyList();
    private static volatile String videoId = "";
    private static long generation, sampleTime, sampleClock, lastSeekClock;
    private static Segment undone;
    private static long undoStart, undoEnd;
    private static boolean adPlaying;
    private static final Runnable TICK = SponsorBlock::tick;

    static {
        // Visibility changes still arrive when playback is paused and no time ticks are emitted.
        PlayerControls.addVisibilityListener(() -> MAIN.post(SponsorBlock::refresh));
        PlayerType.onChange.add(type -> MAIN.post(SponsorBlock::refresh));
    }

    private static void refresh() {
        MAIN.removeCallbacks(TICK);
        tick();
    }

    private static final class CacheEntry {
        final List<Segment> segments;
        final long expires = SystemClock.elapsedRealtime() + 5 * 60_000;
        CacheEntry(List<Segment> segments) { this.segments = segments; }
    }
    private SponsorBlock() {}

    public static void attach(Activity activity) {
        SponsorBlockOverlay existing = overlay.get();
        if (existing != null && existing.getContext() == activity) return;
        SponsorBlockOverlay view = new SponsorBlockOverlay(activity);
        ((ViewGroup) activity.getWindow().getDecorView()).addView(view,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay = new WeakReference<>(view);
        View bounds = player.get();
        if (bounds != null) view.alignTo(bounds);
    }

    public static void playerView(View view) {
        View previous = player.get();
        if (previous == view) return;
        if (previous != null) previous.removeOnLayoutChangeListener(PLAYER_LAYOUT);
        player = new WeakReference<>(view);
        view.addOnLayoutChangeListener(PLAYER_LAYOUT);
        SponsorBlockOverlay controls = overlay.get();
        if (controls != null) controls.alignTo(view);
    }

    public static void initialize(VideoInformation.PlaybackController ignored) {
        MAIN.post(() -> {
            // YouTube can create another controller after publishing the video's ID. The
            // ID callback, not controller construction, owns segment/cache invalidation.
            adPlaying = false;
            MAIN.removeCallbacks(TICK);
            updateOverlay(null);
        });
    }

    public static void newVideoLoaded(String id) {
        if (id == null || id.isEmpty()) return;
        MAIN.post(() -> {
            if (id.equals(videoId)) return;
            videoId = id;
            Logger.debug(() -> "SponsorBlock video: " + id);
            long requestGeneration = ++generation;
            segments = Collections.emptyList();
            skipped.clear();
            counted.clear();
            undone = null;
            MAIN.removeCallbacks(TICK);
            updateOverlay(null);
            if (!Settings.getBoolean("sb_enabled", true) || VideoInformation.lastVideoIdIsShort()) return;
            CacheEntry cached = CACHE.get(id);
            if (cached != null && cached.expires > SystemClock.elapsedRealtime()) {
                segments = cached.segments;
                return;
            }
            SponsorBlockApi.execute(() -> {
                try {
                    List<Segment> loaded = SponsorBlockApi.fetch(id);
                    MAIN.post(() -> {
                        CACHE.put(id, new CacheEntry(loaded));
                        if (generation != requestGeneration || !id.equals(videoId)) return;
                        segments = loaded;
                        Logger.debug(() -> "SponsorBlock loaded " + loaded.size() + " segments");
                        setVideoTime(VideoInformation.getVideoTime());
                    });
                } catch (Exception error) {
                    Logger.error(() -> "SponsorBlock fetch: " + error);
                    if (Settings.getBoolean("sb_toast_on_connection_error", true) && id.equals(videoId)) {
                        SponsorBlockApi.toast("Could not load SponsorBlock segments");
                    }
                }
            });
        });
    }

    public static void setVideoTime(long time) {
        MAIN.post(() -> {
            sampleTime = time;
            sampleClock = SystemClock.elapsedRealtime();
            MAIN.removeCallbacks(TICK);
            tick();
        });
    }

    public static void setAdVisibility(int visibility) {
        MAIN.post(() -> { adPlaying = visibility == View.VISIBLE; if (adPlaying) updateOverlay(null); });
    }

    static boolean active() {
        return Settings.getBoolean("sb_enabled", true) && !adPlaying
                && !VideoInformation.lastVideoIdIsShort() && !ShortsPlayerState.isOpen()
                && videoId.equals(VideoInformation.getVideoId());
    }

    static List<Segment> currentSegments() { return segments; }
    static String currentVideoId() { return videoId; }

    private static long estimatedTime() {
        long elapsed = VideoState.current() == VideoState.PLAYING
                ? Math.min(1500, SystemClock.elapsedRealtime() - sampleClock) : 0;
        return sampleTime + (long) (elapsed * VideoInformation.getPlaybackSpeed());
    }

    private static void tick() {
        if (!active() || sampleTime < 0) { updateOverlay(null); return; }
        long now = estimatedTime();
        long next = Long.MAX_VALUE;
        Segment manual = null;
        if (undone != null && (now < undoStart || now >= undoEnd)) undone = null;
        for (Segment segment : segments) {
            if (segment.ignored() || segment.end - segment.start < minimumDuration() && !segment.highlight()) continue;
            if (segment.highlight()) {
                if (now < segment.start && !skipped.contains(segment.uuid)) {
                    if (segment.automatic() && undone != segment) { skip(segment); return; }
                    if (!"seekbar-only".equals(segment.category.mode())) manual = segment;
                }
                continue;
            }
            if (now < segment.start) { next = Math.min(next, segment.start); continue; }
            if (now >= segment.end || undone != null && segment.start < undoEnd && segment.end > undoStart) continue;
            if (segment.automatic() && !("skip-once".equals(segment.category.mode()) && skipped.contains(segment.uuid))) {
                if (SystemClock.elapsedRealtime() - lastSeekClock > 1000) { skip(segment); return; }
            } else if (!"seekbar-only".equals(segment.category.mode())) {
                manual = segment;
            }
        }
        updateOverlay(manual);
        if (VideoState.current() == VideoState.PLAYING && next != Long.MAX_VALUE) {
            long delay = (long) ((next - now) / Math.max(.1f, VideoInformation.getPlaybackSpeed()));
            MAIN.postDelayed(TICK, Math.max(20, Math.min(delay, 1000)));
        }
    }

    static void skip(Segment segment) {
        if (!active()) return;
        long from = estimatedTime();
        long target = segment.highlight() ? segment.start : segment.end;
        // Merge touching/overlapping automatic segments, so a skip cannot land inside another.
        if (!segment.highlight()) {
            target = SegmentTimeline.skipEnd(segments, target, other ->
                    !other.highlight() && !other.ignored() && other.automatic() && other != undone
                    && !("skip-once".equals(other.category.mode()) && skipped.contains(other.uuid))
                    && other.end - other.start >= minimumDuration());
        }
        long duration = VideoInformation.getVideoLength();
        if (duration > 0) target = Math.min(target, Math.max(0, duration - 250));
        if (target <= from) return;
        if (!VideoInformation.seekTo(target)) return;
        lastSeekClock = SystemClock.elapsedRealtime();
        sampleTime = target;
        sampleClock = lastSeekClock;
        skipped.add(segment.uuid);
        long saved = Math.max(0, target - from);
        for (Segment other : segments) {
            if (other.end > from && other.end <= target && other.automatic()) skipped.add(other.uuid);
        }
        if (counted.add(segment.uuid) && Settings.getBoolean("sb_track_skip_count", true)) {
            long count = storedLong("sb_local_skip_count");
            long milliseconds = storedLong("sb_local_time_saved");
            ReseamSettings.setString("you_tube_settings.sb_local_skip_count", Long.toString(count + 1));
            ReseamSettings.setString("you_tube_settings.sb_local_time_saved", Long.toString(milliseconds + saved));
            SponsorBlockApi.execute(() -> {
                try { SponsorBlockApi.request("POST", "/api/viewedVideoSponsorTime", "UUID", segment.uuid); }
                catch (Exception error) { Logger.debug(() -> "SponsorBlock view count: " + error); }
            });
        }
        if (Settings.getBoolean("sb_toast_on_skip", true)) SponsorBlockApi.toast("Skipped " + segment.category.title.toLowerCase());
        SponsorBlockOverlay view = overlay.get();
        if (view != null) view.offerUndo(segment, from, target);
        MAIN.removeCallbacks(TICK);
        MAIN.postDelayed(TICK, 1100);
    }

    static void undo(Segment segment, long time) {
        undo(segment, time, segment.end);
    }

    static void undo(Segment segment, long time, long skippedTo) {
        if (!segments.contains(segment) || !active()) return;
        if (!VideoInformation.seekTo(time)) return;
        undone = segment;
        undoStart = Math.min(time, segment.start);
        undoEnd = Math.max(segment.end, skippedTo);
        sampleTime = time;
        sampleClock = SystemClock.elapsedRealtime();
    }

    private static void updateOverlay(Segment segment) {
        SponsorBlockOverlay view = overlay.get();
        if (view != null) view.update(segment, active());
    }

    static long storedLong(String name) {
        try { return Long.parseLong(ReseamSettings.getString("you_tube_settings." + name, "0")); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private static long minimumDuration() {
        try { return (long) (Math.max(0, Double.parseDouble(Settings.getString("sb_min_segment_duration", "0"))) * 1000); }
        catch (NumberFormatException ignored) { return 0; }
    }

    public static void drawSegments(Canvas canvas, Rect bounds, float centerY, float radius) {
        if (!active() || bounds == null || bounds.width() <= 0) return;
        long length = VideoInformation.getVideoLength();
        if (length <= 0) return;
        float halfHeight = Math.max(2, radius / 3);
        for (Segment segment : segments) {
            if (segment.ignored()) continue;
            float left = bounds.left + bounds.width() * Math.min(1f, (float) segment.start / length);
            float right = bounds.left + bounds.width() * Math.min(1f, (float) segment.end / length);
            PAINT.setColor(segment.category.color());
            canvas.drawRect(left, centerY - halfHeight, Math.max(left + 2, right), centerY + halfHeight, PAINT);
        }
    }

    public static String appendTime(String original) {
        if (!active() || !Settings.getBoolean("sb_video_length_without_segments", false)) return original;
        long length = VideoInformation.getVideoLength();
        long removed = SegmentTimeline.removedDuration(segments, length,
                segment -> !segment.ignored() && !segment.highlight());
        if (removed == 0) return original;
        return original + " (" + android.text.format.DateUtils.formatElapsedTime(Math.max(0, length - removed) / 1000) + ")";
    }
}
