// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import android.os.SystemClock
import android.view.View
import app.reseam.runtime.Export
import app.reseam.youtube.player.ShortsPlayer
import app.reseam.youtube.player.VideoState
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.video.VideoInformation

internal object SponsorBlockPlayback {
    private data class Sample(val time: Long = 0, val clock: Long = 0)

    private var sample = Sample()
    var lastSeekClock = 0L
    private var adPlaying = false
    val tickTask = Runnable(::tick)

    fun refresh() {
        sponsorBlockMain.removeCallbacks(tickTask)
        tick()
    }

    @Export
    @JvmStatic
    fun initialize(controller: VideoInformation.PlaybackController) {
        sponsorBlockMain.post {
            // Controller construction can follow the ID callback; only the ID invalidates segments.
            adPlaying = false
            sponsorBlockMain.removeCallbacks(tickTask)
            updateOverlay(null)
        }
    }

    @Export
    @JvmStatic
    fun setVideoTime(time: Long) {
        sponsorBlockMain.post {
            sampleAt(time)
            sponsorBlockMain.removeCallbacks(tickTask)
            tick()
        }
    }

    @Export
    @JvmStatic
    fun setAdVisibility(visibility: Int) {
        sponsorBlockMain.post {
            adPlaying = visibility == View.VISIBLE
            if (adPlaying) updateOverlay(null)
        }
    }

    internal val isActive: Boolean
        get() =
            VideoSettings.sbEnabled.value &&
                !adPlaying &&
                !VideoInformation.lastVideoIdIsShort &&
                !ShortsPlayer.isOpen &&
                SponsorBlockSession.videoId == VideoInformation.videoId

    val estimatedTime: Long
        get() {
            val elapsed =
                if (VideoState.current == VideoState.PLAYING) {
                    minOf(1500L, SystemClock.elapsedRealtime() - sample.clock)
                } else 0L
            return sample.time + (elapsed * VideoInformation.playbackSpeed).toLong()
        }

    private fun tick() {
        if (!isActive || sample.time < 0) {
            updateOverlay(null)
            return
        }
        val now = estimatedTime
        SponsorBlockSkips.expireUndo(now)
        val decisions =
            SponsorBlockSession.segments.map { segment ->
                when (val decision = segment.playbackDecision(now)) {
                    is PlaybackDecision.Skip -> {
                        SponsorBlockSkips.skip(decision.segment)
                        return
                    }
                    else -> decision
                }
            }
        updateOverlay(decisions.filterIsInstance<PlaybackDecision.Offer>().lastOrNull()?.segment)
        val next =
            decisions.filterIsInstance<PlaybackDecision.WakeAt>().minOfOrNull { it.time }
                ?: Long.MAX_VALUE
        if (VideoState.current == VideoState.PLAYING && next != Long.MAX_VALUE) {
            val delay = ((next - now) / maxOf(.1f, VideoInformation.playbackSpeed)).toLong()
            sponsorBlockMain.postDelayed(tickTask, delay.coerceIn(20L, 1000L))
        }
    }

    fun sampleAt(time: Long, clock: Long = SystemClock.elapsedRealtime()) {
        sample = Sample(time, clock)
    }

    val minimumDuration: Long
        get() =
            (maxOf(0.0, VideoSettings.sbMinSegmentDuration.value.toDoubleOrNull() ?: 0.0) * 1000)
                .toLong()
}
