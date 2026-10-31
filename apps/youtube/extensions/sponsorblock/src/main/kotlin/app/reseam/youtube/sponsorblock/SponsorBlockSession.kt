// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import android.os.SystemClock
import android.util.LruCache
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.showToast
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.video.VideoInformation

internal object SponsorBlockSession {
    private val cache = LruCache<String, CacheEntry>(100)
    @Volatile
    var segments: List<Segment> = emptyList()
        private set

    @Volatile
    var videoId = ""
        private set

    private var generation = 0L

    private data class CacheEntry(
        val segments: List<Segment>,
        val expiresAt: Long = SystemClock.elapsedRealtime() + 5 * 60_000,
    )

    @Export
    @JvmStatic
    fun newVideoLoaded(id: String?) {
        if (id.isNullOrEmpty()) return
        sponsorBlockMain.post {
            if (id == videoId) return@post
            videoId = id
            val requestGeneration = ++generation
            segments = emptyList()
            SponsorBlockSkips.reset()
            sponsorBlockMain.removeCallbacks(SponsorBlockPlayback.tickTask)
            updateOverlay(null)
            if (!VideoSettings.sbEnabled.value || VideoInformation.lastVideoIdIsShort) return@post
            val cached = cache[id]?.takeIf { it.expiresAt > SystemClock.elapsedRealtime() }
            if (cached == null) fetchSegments(id, requestGeneration) else segments = cached.segments
        }
    }

    private fun fetchSegments(id: String, requestGeneration: Long) {
        executeSponsorBlockRequest {
            try {
                val loaded = fetchSponsorBlockSegments(id)
                sponsorBlockMain.post {
                    cache.put(id, CacheEntry(loaded))
                    if (generation == requestGeneration && id == videoId) {
                        segments = loaded
                        SponsorBlockPlayback.setVideoTime(VideoInformation.videoTime)
                    }
                }
            } catch (error: Exception) {
                Logger.error(error) { "SponsorBlock fetch" }
                if (VideoSettings.sbToastOnConnectionError.value && id == videoId) {
                    showToast(YouTubeStrings.sponsorblockLoadFailed)
                }
            }
        }
    }
}
