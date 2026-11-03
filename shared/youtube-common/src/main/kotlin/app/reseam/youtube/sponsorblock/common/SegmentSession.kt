// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock.common

import android.os.SystemClock
import android.util.LruCache
import app.reseam.bundle.Logger
import app.reseam.bundle.mainThreadHandler
import app.reseam.youtube.core.showToast
import app.reseam.youtubecommon.settings.SponsorBlockSettings
import app.reseam.youtubecommon.settings.YouTubeCommonStrings

// The segments of the open video. Opened and loaded on the main thread, which also receives them.
class SegmentSession(
    private val categories: List<Segment.Category>,
    private val loaded: () -> Unit,
) {
    private class CacheEntry(
        val segments: List<Segment>,
        val expiresAt: Long = SystemClock.elapsedRealtime() + 5 * 60_000,
    )

    private val main = mainThreadHandler()
    private val cache = LruCache<String, CacheEntry>(100)

    @Volatile
    var videoId = ""
        private set

    @Volatile
    var segments: List<Segment> = emptyList()
        private set

    /** False when [id] is already the open video. */
    fun open(id: String): Boolean {
        if (id == videoId) return false
        videoId = id
        segments = emptyList()
        return true
    }

    fun load() {
        val id = videoId
        val cached = cache[id]?.takeIf { it.expiresAt > SystemClock.elapsedRealtime() }
        if (cached != null) return show(cached.segments)
        executeSponsorBlockRequest {
            try {
                val fetched = fetchSponsorBlockSegments(id, categories)
                main.post {
                    cache.put(id, CacheEntry(fetched))
                    if (id == videoId) show(fetched)
                }
            } catch (error: Exception) {
                Logger.error(error) { "SponsorBlock fetch" }
                main.post {
                    if (SponsorBlockSettings.sbToastOnConnectionError.value && id == videoId)
                        showToast(YouTubeCommonStrings.sponsorblockLoadFailed)
                }
            }
        }
    }

    private fun show(fetched: List<Segment>) {
        segments = fetched
        loaded()
    }
}
