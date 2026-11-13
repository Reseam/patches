// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock.common

import android.net.Uri
import app.reseam.bundle.Logger
import app.reseam.bundle.ReseamPatches
import app.reseam.bundle.openHttp
import app.reseam.youtube.core.debug
import app.reseam.youtube.core.readResponseText
import app.reseam.youtube.core.showToast
import app.reseam.youtubecommon.settings.SponsorBlockSettings
import app.reseam.youtubecommon.settings.YouTubeCommonStrings
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

private val network =
    ThreadPoolExecutor(
        2,
        2,
        30,
        TimeUnit.SECONDS,
        ArrayBlockingQueue(16),
        { runnable -> Thread(runnable, "Reseam-SponsorBlock").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy(),
    )
@Volatile private var backoffUntil = 0L

fun executeSponsorBlockRequest(task: () -> Unit) {
    try {
        network.execute(task)
    } catch (_: RejectedExecutionException) {
        Logger.debug { "SponsorBlock request queue is full" }
    }
}

fun requestSponsorBlock(
    method: String,
    path: String,
    vararg query: Pair<String, String>,
): String {
    if (System.currentTimeMillis() < backoffUntil)
        throw IOException("SponsorBlock is temporarily rate limited")
    val uri = Uri.parse(SponsorBlockSettings.sbApiUrl.value)
    if (uri.scheme != "https" || uri.host == null)
        throw IOException("SponsorBlock API URL must use HTTPS")
    val builder = uri.buildUpon().appendEncodedPath(path.removePrefix("/"))
    query.forEach { (key, value) -> builder.appendQueryParameter(key, value) }
    return openHttp(builder.build().toString()) {
        requestMethod = method
        connectTimeout = 4000
        readTimeout = 7000
        setRequestProperty("User-Agent", "Reseam/YouTube")
        if (method == "POST") {
            doOutput = true
            setFixedLengthStreamingMode(0)
        }
        val status = responseCode
        if (status == 404 && path == "/api/skipSegments" && method == "GET") return@openHttp "[]"
        if (status == 429) backoffUntil = System.currentTimeMillis() + 10 * 60_000
        if (status !in 200..299) throw IOException("SponsorBlock HTTP $status")
        readResponseText(1_048_576, "SponsorBlock response is too large")
    }
}

fun fetchSponsorBlockSegments(videoId: String, categories: List<Segment.Category>): List<Segment> {
    // The API leaves out mute segments unless they are asked for; they are skipped like the rest.
    val actionTypes =
        listOfNotNull("skip", "mute", "poi".takeIf { Segment.Category.HIGHLIGHT in categories })
    val response =
        requestSponsorBlock(
            "GET",
            "/api/skipSegments",
            "videoID" to videoId,
            "categories" to JSONArray(categories.map { it.key }).toString(),
            "actionTypes" to JSONArray(actionTypes).toString(),
        )
    val data = JSONArray(response)
    return (0 until data.length())
        .mapNotNull { data.getJSONObject(it).toSegment(categories) }
        .sortedBy { it.start }
}

private fun JSONObject.toSegment(categories: List<Segment.Category>): Segment? {
    val category = categories.firstOrNull { it.key == optString("category") } ?: return null
    val times = optJSONArray("segment")?.takeIf { it.length() == 2 } ?: return null
    val from = times.getDouble(0)
    val to = times.getDouble(1)
    if (!from.isFinite() || !to.isFinite() || from < 0 || to < from) return null
    return Segment(getString("UUID"), category, Math.round(from * 1000), Math.round(to * 1000))
}

fun reportSponsorBlockSkip(segment: Segment, saved: Long, firstSkip: Boolean) {
    if (firstSkip && SponsorBlockSettings.sbTrackSkipCount.value) recordSkip(segment.uuid, saved)
    if (SponsorBlockSettings.sbToastOnSkip.value) {
        showToast(
            YouTubeCommonStrings.sponsorblockSkipped(
                segment.category.title.lowercase(Locale.getDefault())
            )
        )
    }
}

private fun recordSkip(uuid: String, saved: Long) {
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
            requestSponsorBlock("POST", "/api/viewedVideoSponsorTime", "UUID" to uuid)
        } catch (error: Exception) {
            Logger.debug(error) { "SponsorBlock view count" }
        }
    }
}
