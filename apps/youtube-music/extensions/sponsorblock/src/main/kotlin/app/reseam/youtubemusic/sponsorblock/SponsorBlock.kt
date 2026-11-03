// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SponsorBlock")

package app.reseam.youtubemusic.sponsorblock

import android.media.session.PlaybackState
import android.os.SystemClock
import app.reseam.bundle.mainThreadHandler
import app.reseam.runtime.Export
import app.reseam.youtube.sponsorblock.common.PlaybackPosition
import app.reseam.youtube.sponsorblock.common.Segment
import app.reseam.youtube.sponsorblock.common.SegmentSession
import app.reseam.youtube.sponsorblock.common.isTooShort
import app.reseam.youtube.sponsorblock.common.playbackDelay
import app.reseam.youtube.sponsorblock.common.reportSponsorBlockSkip
import app.reseam.youtube.sponsorblock.common.segmentSkipEnd
import java.lang.ref.WeakReference

// State is only touched on the main thread; the player reports from its own threads.
private val main = mainThreadHandler()

// A highlight only offers a jump to it, which needs the skip button YouTube Music lacks.
private val session = SegmentSession(Segment.Category.entries - Segment.Category.HIGHLIGHT, ::check)
private val position = PlaybackPosition()
private var player = WeakReference<Any>(null)
// The client playback nonce of the video's playback. Progress reports carry one too, which tells a
// new video's first reports apart from the old video's, and the video's from an ad's.
private var videoCpn = ""
private var reported = false
private var length = 0L
private var seekTarget = -1L
private var seekClock = 0L
private var playing = true
private var speed = 1f
private val skipped = mutableSetOf<String>()
private val checkTask = Runnable(::check)

@Export external fun seekTo(player: Any, time: Long): Boolean

@Export
fun setPlayer(instance: Any) {
    main.post { player = WeakReference(instance) }
}

@Export
fun setVideo(id: String?, cpn: String?) {
    if (id.isNullOrEmpty() || cpn.isNullOrEmpty()) return
    main.post {
        if (cpn != videoCpn) {
            videoCpn = cpn
            reported = false
            length = 0
            seekTarget = -1
            skipped.clear()
            main.removeCallbacks(checkTask)
        }
        if (session.open(id)) session.load()
    }
}

// The media session carries the state the player is in wherever it plays, such as in the background
// or on Android Auto. Progress reports stop while paused, without saying so.
@Export
fun setPlaybackState(state: PlaybackState) {
    main.post {
        playing = state.state == PlaybackState.STATE_PLAYING
        if (state.playbackSpeed > 0) speed = state.playbackSpeed
        check()
    }
}

@Export
fun setVideoTime(time: Long, windowStart: Long, windowEnd: Long, cpn: String?) {
    main.post {
        if (cpn != videoCpn) return@post
        length = windowEnd - windowStart
        val clock = SystemClock.elapsedRealtime()
        // Reports queued before a seek arrive after it, until the first one from its target on.
        if (seekTarget >= 0) {
            if (time < seekTarget && clock - seekClock < 1000) return@post
            seekTarget = -1
        }
        position.sample(time, clock)
        reported = true
        check()
    }
}

private fun check() {
    main.removeCallbacks(checkTask)
    if (!reported) return
    val now = position.estimate(playing, speed)
    val skippable = session.segments.filter { it.skippable }
    val current = skippable.firstOrNull { now >= it.start && now < it.end }
    if (current != null) return skip(current, now)
    val next = skippable.firstOrNull { it.start > now } ?: return
    val delay = playbackDelay(now, next.start, speed)
    if (playing && delay < 1000) main.postDelayed(checkTask, delay)
}

// The skip button and seekbar YouTube offers for the other behaviors don't exist here, so their
// segments play. A segment set to skip once plays when the listener seeks back into it, until the
// song is played again.
private val Segment.skippable: Boolean
    get() =
        !isTooShort &&
            when (category.mode) {
                "skip" -> true
                "skip-once" -> uuid !in skipped
                else -> false
            }

private fun skip(segment: Segment, now: Long) {
    val instance = player.get() ?: return
    val end = segmentSkipEnd(session.segments, segment.end) { it.skippable }
    // The player stalls when seeking to the very end, instead of moving on to the next song.
    val target = if (length > 0) minOf(end, length - 250) else end
    if (target <= now || !seekTo(instance, target)) return
    seekTarget = target
    seekClock = SystemClock.elapsedRealtime()
    position.sample(target, seekClock)
    reportSponsorBlockSkip(segment, target - now, segment.uuid !in skipped)
    session.segments.filter { it.skippable && it.start < target && it.end > now }.mapTo(skipped) {
        it.uuid
    }
}
