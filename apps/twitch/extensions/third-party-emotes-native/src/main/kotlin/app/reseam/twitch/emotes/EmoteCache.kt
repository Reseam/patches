// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.emotes

import android.content.Context
import app.reseam.bundle.Logger
import app.reseam.bundle.openHttp
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONException

// Channels change their emotes rarely, so a cached list shows at once and refreshes after this long.
private const val FRESH_MILLIS = 60 * 60 * 1000L
private const val TIMEOUT_MILLIS = 10_000

private val emotesByKey = ConcurrentHashMap<String, List<Emote>>()
private val refreshingKeys = ConcurrentHashMap.newKeySet<String>()
private val refresher = Executors.newSingleThreadExecutor()

/** The emotes that [provider] has for [channelId], from memory or the cache file, or null until its first answer arrives. */
internal fun Context.emotes(provider: Provider, channelId: String): List<Emote>? {
    val key = "${provider.name.lowercase()}/${channelId.ifEmpty { "global" }}"
    emotesByKey[key]?.let { return it }
    val file = File(cacheDir, "reseam-emotes/$key.json")
    val cached = if (file.exists()) read(file) else null
    if (cached == null || System.currentTimeMillis() - file.lastModified() > FRESH_MILLIS) refresh(key, file, provider, channelId)
    return cached?.also { emotesByKey[key] = it }
}

private fun read(file: File): List<Emote>? =
    try {
        JSONArray(file.readText()).let { array -> (0 until array.length()).map { Emote.fromJson(array.getJSONObject(it)) } }
    } catch (error: IOException) {
        Logger.warn(error) { "Third-party emotes: could not read ${file.name}" }
        null
    } catch (error: JSONException) {
        Logger.warn(error) { "Third-party emotes: could not read ${file.name}" }
        null
    }

// A failed request leaves the channel without the provider's emotes until the next start, and keeps any older cache file.
private fun refresh(key: String, file: File, provider: Provider, channelId: String) {
    if (!refreshingKeys.add(key)) return
    refresher.execute {
        val emotes = fetch(provider, channelId)
        if (emotes != null) {
            file.parentFile?.mkdirs()
            file.writeText(JSONArray(emotes.map(Emote::toJson)).toString())
            emotesByKey[key] = emotes
        } else {
            emotesByKey.putIfAbsent(key, emptyList())
        }
        refreshingKeys.remove(key)
    }
}

// A channel without an account at the provider answers 404, which means no emotes.
private fun fetch(provider: Provider, channelId: String): List<Emote>? =
    try {
        openHttp(provider.url(channelId)) {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> provider.parse(inputStream.bufferedReader().use { it.readText() }, channelId)
                HttpURLConnection.HTTP_NOT_FOUND -> emptyList()
                else -> null
            }
        }
    } catch (error: IOException) {
        Logger.warn(error) { "Third-party emotes: ${provider.name} did not answer for ${channelId.ifEmpty { "global" }}" }
        null
    } catch (error: JSONException) {
        Logger.warn(error) { "Third-party emotes: ${provider.name} answered an unknown format" }
        null
    }
