// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import android.net.Uri
import app.reseam.bundle.Logger
import app.reseam.bundle.ReseamPatches
import app.reseam.bundle.openHttp
import app.reseam.youtube.core.debug
import app.reseam.youtube.core.readResponseText
import app.reseam.youtube.settings.VideoSettings
import java.io.IOException
import java.util.UUID
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

internal fun executeSponsorBlockRequest(task: () -> Unit) {
    try {
        network.execute(task)
    } catch (_: RejectedExecutionException) {
        Logger.debug { "SponsorBlock request queue is full" }
    }
}

internal fun fetchSponsorBlockSegments(videoId: String): List<Segment> {
    val categories = JSONArray().apply { Segment.Category.entries.forEach { put(it.key) } }
    val response =
        requestSponsorBlock(
            "GET",
            "/api/skipSegments",
            "videoID" to videoId,
            "categories" to categories.toString(),
            "actionTypes" to "[\"skip\",\"poi\"]",
        )
    val data = JSONArray(response)
    return (0 until data.length())
        .mapNotNull { data.getJSONObject(it).toSegment() }
        .sortedBy { it.start }
}

private fun JSONObject.toSegment(): Segment? {
    val category =
        Segment.Category.entries.firstOrNull { it.key == optString("category") } ?: return null
    val times = optJSONArray("segment")?.takeIf { it.length() == 2 } ?: return null
    val from = times.getDouble(0)
    val to = times.getDouble(1)
    if (!from.isFinite() || !to.isFinite() || from < 0 || to < from) return null
    return Segment(getString("UUID"), category, Math.round(from * 1000), Math.round(to * 1000))
}

internal fun requestSponsorBlock(
    method: String,
    path: String,
    vararg query: Pair<String, String>,
): String {
    if (System.currentTimeMillis() < backoffUntil)
        throw IOException("SponsorBlock is temporarily rate limited")
    val uri = Uri.parse(VideoSettings.sbApiUrl.value)
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

// Persisted feature data with no declaration in the settings DSL.
private const val USER_ID_KEY = "you_tube_settings.sb_private_user_id"
private val userIdLock = Any()

internal fun sponsorBlockUserId(): String =
    synchronized(userIdLock) {
        val settings = ReseamPatches.bundle().settings
        settings.getString(USER_ID_KEY, "").ifEmpty {
            "${UUID.randomUUID()}${UUID.randomUUID()}".replace("-", "").also {
                settings.setString(USER_ID_KEY, it)
            }
        }
    }
