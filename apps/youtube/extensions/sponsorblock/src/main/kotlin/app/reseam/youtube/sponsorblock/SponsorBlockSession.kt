// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import app.reseam.runtime.Export
import app.reseam.youtube.sponsorblock.common.Segment
import app.reseam.youtube.sponsorblock.common.SegmentSession
import app.reseam.youtube.video.VideoInformation
import app.reseam.youtubecommon.settings.SponsorBlockSettings

internal object SponsorBlockSession {
    private val session =
        SegmentSession(Segment.Category.entries) {
            SponsorBlockPlayback.setVideoTime(VideoInformation.videoTime)
        }

    val segments
        get() = session.segments

    val videoId
        get() = session.videoId

    @Export
    @JvmStatic
    fun newVideoLoaded(id: String?) {
        if (id.isNullOrEmpty()) return
        sponsorBlockMain.post {
            if (!session.open(id)) return@post
            SponsorBlockSkips.reset()
            sponsorBlockMain.removeCallbacks(SponsorBlockPlayback.tickTask)
            updateOverlay(null)
            if (!SponsorBlockSettings.sbEnabled.value || VideoInformation.lastVideoIdIsShort)
                return@post
            session.load()
        }
    }
}
