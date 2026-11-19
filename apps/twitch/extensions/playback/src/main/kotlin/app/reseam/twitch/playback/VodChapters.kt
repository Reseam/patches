// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("VodChapters")

package app.reseam.twitch.playback

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import app.reseam.runtime.Export
import app.reseam.twitch.settings.TwitchStrings
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.WeakHashMap
import kotlin.concurrent.thread
import org.json.JSONObject

// Twitch's app never queries chapters, but its GraphQL API returns a video's chapter markers to the web client ID without a login.
private const val GQL_URL = "https://gql.twitch.tv/gql"
private const val WEB_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
private const val CHAPTERS_QUERY =
    "query(\$id: ID!) { video(id: \$id) { moments(momentRequestType: VIDEO_CHAPTER_MARKERS) { edges { node { positionMilliseconds description } } } } }"
private const val TIMEOUT_MILLIS = 10_000

private class Chapter(val positionMillis: Int, val title: String)

// One video plays at a time, so the chapters of the last one are enough. The main thread sets all three.
private var loadedVodId: String? = null
private var loadedChapters: List<Chapter> = emptyList()
private var loadingVodId: String? = null

// A detached button never runs what it posts, so results come back through the main looper instead.
private val mainThread = Handler(Looper.getMainLooper())

// A button keeps the presenter it found until the next player view registers, which can change the nearest one.
private val buttonPresenters = WeakHashMap<View, WeakReference<Any>>()

@Export
fun trackPlayer(playerView: View?, presenter: Any?) {
    registerPlayer(playerView, presenter)
    buttonPresenters.clear()
}

/** Puts a Chapters button with [icon] just before the native player's settings [gear], shown while the video has two chapters or more. */
@Export
fun addChaptersButton(gear: View?, icon: Int) {
    if (gear !is ImageView) return
    val chapters = gearStyledButton(gear, icon, TwitchStrings.vodChapters)
    // The list icon is black, so it takes the gear's tint, or white when the gear has none.
    if (chapters.imageTintList == null) chapters.imageTintList = ColorStateList.valueOf(Color.WHITE)
    chapters.setOnClickListener { view -> presenterOf(view)?.let { showChapters(view, it) } }
    addBeforeGear(gear, chapters) { hasChapters(chapters) }
}

/** Returns the id of the video [presenter] plays, or null when it plays none. */
@Export
external fun vodId(presenter: Any?): String?

/** Seeks [presenter]'s video to [positionMillis], the way Twitch jumps to a time itself. */
@Export
external fun seek(presenter: Any?, positionMillis: Int)

private fun presenterOf(button: View): Any? =
    buttonPresenters[button]?.get() ?: presenterFor(button)?.also { buttonPresenters[button] = WeakReference(it) }

private fun hasChapters(button: View): Boolean {
    val vodId = presenterOf(button)?.let(::vodId) ?: return false
    if (vodId != loadedVodId) {
        load(vodId, button)
        return false
    }
    return loadedChapters.size > 1
}

private fun load(vodId: String, button: View) {
    if (vodId == loadingVodId) return
    loadingVodId = vodId
    val shownOn = WeakReference(button)
    thread(isDaemon = true, name = "reseam-vod-chapters") {
        val chapters = runCatching { fetchChapters(vodId) }.getOrDefault(emptyList())
        mainThread.post {
            // A newer video may have started loading before this one's chapters arrived.
            if (vodId != loadingVodId) return@post
            loadedChapters = chapters
            loadedVodId = vodId
            (shownOn.get()?.parent as? View)?.invalidate()
        }
    }
}

private fun fetchChapters(vodId: String): List<Chapter> {
    val connection = URL(GQL_URL).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        connection.doOutput = true
        connection.setRequestProperty("Client-ID", WEB_CLIENT_ID)
        connection.setRequestProperty("Content-Type", "application/json")
        // A VodModel id starts with a v, which Twitch's own player strips before it requests the video, and the API takes the number.
        val variables = JSONObject().put("id", vodId.removePrefix("v"))
        connection.outputStream.use { it.write(JSONObject().put("query", CHAPTERS_QUERY).put("variables", variables).toString().toByteArray()) }
        val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val edges = response.optJSONObject("data")?.optJSONObject("video")?.optJSONObject("moments")?.optJSONArray("edges") ?: return emptyList()
        return (0 until edges.length()).mapNotNull { index ->
            edges.optJSONObject(index)?.optJSONObject("node")?.let { node -> Chapter(node.optInt("positionMilliseconds"), node.optString("description")) }
        }
    } finally {
        connection.disconnect()
    }
}

private fun showChapters(button: View, presenter: Any) {
    val chapters = loadedChapters
    if (vodId(presenter) != loadedVodId || chapters.isEmpty()) return
    val titles = chapters.map { "${clockTime(it.positionMillis)}   ${it.title}" }.toTypedArray()
    AlertDialog.Builder(button.context)
        .setTitle(TwitchStrings.vodChapters)
        .setItems(titles) { _, index -> seek(presenter, chapters[index].positionMillis) }
        .show()
}

private fun clockTime(millis: Int): String {
    val seconds = millis / 1000
    return String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
}
