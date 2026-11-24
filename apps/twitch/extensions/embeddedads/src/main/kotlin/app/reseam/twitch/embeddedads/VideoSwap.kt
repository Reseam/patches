// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("VideoSwap")

package app.reseam.twitch.embeddedads

import android.net.Uri
import app.reseam.bundle.Logger
import app.reseam.bundle.openHttp
import app.reseam.bundle.send
import app.reseam.runtime.Export
import app.reseam.twitch.settings.AdsSettings
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.util.Collections
import java.util.UUID
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import org.json.JSONException
import org.json.JSONObject

private const val GQL_URL = "https://gql.twitch.tv/gql"
private const val TOKEN_QUERY =
    $$"query($login: String!, $params: PlaybackAccessTokenParams!) { streamPlaybackAccessToken(channelName: $login, params: $params) { value signature authorization { isForbidden } } }"
private const val TIMEOUT_MILLIS = 1_500

// The player polls a playlist every 2 seconds and stalls when an answer comes much later.
private const val SEARCH_MILLIS = 3_000L

// Markers can flicker at the end of a break, so the stream returns after this many clean polls.
private const val CLEAN_POLLS = 2
private const val KEPT_STREAMS = 16
private const val STREAM_INFO = "#EXT-X-STREAM-INF:"
private const val SEGMENT = "#EXTINF:"
private const val LIVE_TITLE = ",live"

// Streams of these players often carry no ads while the app's own stream does. Autoplay goes last because it stops at 360p.
private val backupPlayers = listOf("web" to "popout", "web" to "embed", "android" to "autoplay")

// Xtra found that a token asked for without a device ID can start on a "commercial break in progress" slate.
private val deviceId = UUID.randomUUID().toString().replace("-", "")

private val resolutionAttribute = Regex("""[:,]RESOLUTION=([^,]*)""")
private val frameRateAttribute = Regex("""[:,]FRAME-RATE=([^,]*)""")
private val codecAttribute = Regex("""[:,]CODECS="([^".,]*)""")

private class Variant(val url: String, val resolution: String?, val frameRate: String?, val codec: String?) {
    val pixels = resolution?.split('x')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }?.let { it[0] * it[1] } ?: 0
}

private class Token(val value: String, val signature: String)

private class Body(val url: String) {
    var bytes: ByteArray? = null
    var offset = 0
}

private class LiveStream(val channel: String, val usherUrl: String, val variants: List<Variant>) {
    private var backup: List<Variant>? = null
    private var nextPlayer = 0
    private var cleanPolls = 0

    /** The media playlist the player gets for [variant] in place of [original]. */
    @Synchronized
    fun playlist(variant: Variant, original: ByteArray): ByteArray {
        if (!hasStitchedAd(String(original, Charsets.UTF_8))) {
            if (backup == null && nextPlayer == 0) return original
            if (++cleanPolls >= CLEAN_POLLS) {
                endBreak()
                return original
            }
        } else {
            cleanPolls = 0
        }
        return backupPlaylist(variant)?.toByteArray(Charsets.UTF_8) ?: original
    }

    private fun backupPlaylist(variant: Variant): String? {
        val deadline = System.currentTimeMillis() + SEARCH_MILLIS
        while (System.currentTimeMillis() < deadline) {
            val variants = backup ?: findBackup(deadline) ?: return null
            val playlist = closest(variants, variant)?.let { fetchText(it.url, deadline) }
            if (playlist != null && !hasStitchedAd(playlist)) return playlist
            // The backup now has ads too, or its playlist stopped answering.
            backup = null
        }
        return null
    }

    private fun findBackup(deadline: Long): List<Variant>? {
        while (nextPlayer < backupPlayers.size && System.currentTimeMillis() < deadline) {
            val (platform, playerType) = backupPlayers[nextPlayer++]
            val token = fetchToken(channel, platform, playerType, deadline) ?: continue
            val variants = fetchText(usherUrlWith(token), deadline)?.let(::variantsOf)
            if (variants.isNullOrEmpty()) continue
            Logger.info { "Swap embedded ads: $channel plays the $platform $playerType stream during an ad break" }
            backup = variants
            return variants
        }
        return null
    }

    private fun endBreak() {
        if (backup != null) Logger.info { "Swap embedded ads: $channel plays its own stream again" }
        backup = null
        nextPlayer = 0
        cleanPolls = 0
    }

    private fun usherUrlWith(token: Token): String {
        val original = Uri.parse(usherUrl)
        val builder = original.buildUpon().clearQuery()
        for (name in original.queryParameterNames) {
            if (name == "token" || name == "sig") continue
            original.getQueryParameters(name).forEach { builder.appendQueryParameter(name, it) }
        }
        return builder.appendQueryParameter("token", token.value).appendQueryParameter("sig", token.signature).build().toString()
    }
}

private val streamsByVariantUrl = ConcurrentHashMap<String, LiveStream>()
private val streamsByUsherUrl = LinkedHashMap<String, LiveStream>(KEPT_STREAMS, 0.75f, true)
private val bodiesByProvider = Collections.synchronizedMap(WeakHashMap<Any, Body>())

@Volatile
private var warnedSource = false

/** Marks the body that [provider] reads as a live playlist when [url] is one. */
@Export
fun onBody(provider: Any?, url: String?) {
    if (provider != null && url != null && (streamsByVariantUrl.containsKey(url) || channelStart(url) >= 0)) bodiesByProvider[provider] = Body(url)
}

/** Whether [provider] reads a live playlist from [source] that Swap embedded ads serves. */
@Export
fun reads(provider: Any?, source: Any?): Boolean {
    if (provider == null || !bodiesByProvider.containsKey(provider)) return false
    if (source is ReadableByteChannel) return true
    // Twitch's own reader then serves the playlist unchanged.
    bodiesByProvider.remove(provider)
    if (!warnedSource) {
        warnedSource = true
        Logger.warn { "Swap embedded ads: the IVS body source is no ReadableByteChannel" }
    }
    return false
}

/** Fills [buffer] from the playlist that [provider] reads from [source], and returns the byte count or -1 at the end. */
@Export
fun writeTo(provider: Any?, source: Any?, buffer: ByteBuffer?): Int {
    val body = provider?.let { bodiesByProvider[it] } ?: return -1
    val bytes = body.bytes ?: playlistOrOriginal(body.url, readAll(source as ReadableByteChannel)).also { body.bytes = it }
    if (body.offset >= bytes.size || buffer == null) {
        bodiesByProvider.remove(provider)
        return -1
    }
    val count = minOf(bytes.size - body.offset, buffer.remaining())
    buffer.put(bytes, body.offset, count)
    body.offset += count
    return count
}

// IVS handles only read errors, so any other error would stop the player.
private fun playlistOrOriginal(url: String, original: ByteArray): ByteArray =
    try {
        playlistFor(url, original)
    } catch (error: RuntimeException) {
        Logger.warn(error) { "Swap embedded ads: kept a playlist as Twitch sent it" }
        original
    }

private fun readAll(source: ReadableByteChannel): ByteArray {
    val output = ByteArrayOutputStream()
    val chunk = ByteBuffer.allocate(16_384)
    while (source.read(chunk) != -1) {
        output.write(chunk.array(), 0, chunk.position())
        chunk.clear()
    }
    return output.toByteArray()
}

private fun playlistFor(url: String, original: ByteArray): ByteArray {
    val channelStart = channelStart(url)
    if (channelStart >= 0) {
        track(url, url.substring(channelStart, url.indexOf(PLAYLIST_EXTENSION, channelStart)), original)
        return original
    }
    val stream = streamsByVariantUrl[url] ?: return original
    val variant = stream.variants.firstOrNull { it.url == url } ?: return original
    // A read moves the stream to the end, so idle preloads fall out first.
    synchronized(streamsByUsherUrl) { streamsByUsherUrl[stream.usherUrl] }
    return stream.playlist(variant, original)
}

private fun track(usherUrl: String, channel: String, master: ByteArray) {
    val variants = variantsOf(String(master, Charsets.UTF_8)).ifEmpty { return }
    val stream = LiveStream(channel.lowercase(), usherUrl, variants)
    synchronized(streamsByUsherUrl) {
        streamsByUsherUrl.put(usherUrl, stream)?.let(::untrack)
        if (streamsByUsherUrl.size > KEPT_STREAMS) streamsByUsherUrl.remove(streamsByUsherUrl.keys.first())?.let(::untrack)
        variants.forEach { streamsByVariantUrl[it.url] = stream }
    }
}

private fun untrack(stream: LiveStream) {
    stream.variants.forEach { streamsByVariantUrl.remove(it.url, stream) }
}

private fun variantsOf(master: String): List<Variant> {
    val lines = master.lines()
    return lines.indices.mapNotNull { index ->
        val info = lines[index]
        val url = lines.getOrNull(index + 1)?.trim()
        if (!info.startsWith(STREAM_INFO) || url.isNullOrEmpty() || url.startsWith("#")) return@mapNotNull null
        Variant(
            url,
            resolutionAttribute.find(info)?.groupValues?.get(1),
            frameRateAttribute.find(info)?.groupValues?.get(1),
            codecAttribute.find(info)?.groupValues?.get(1),
        )
    }
}

/** The backup variant of the same codec closest in size and frame rate to [wanted], or null when none shares its codec. */
private fun closest(variants: List<Variant>, wanted: Variant): Variant? =
    variants
        .filter { it.codec == wanted.codec }
        .minWithOrNull(compareBy({ abs(it.pixels - wanted.pixels) }, { it.frameRate != wanted.frameRate }))

// A stitched ad replaces live segments, whose titles say live, and Twitch tags it as stitched.
private fun hasStitchedAd(playlist: String): Boolean =
    (playlist.contains("stitched") || playlist.contains("X-TV-TWITCH-AD")) &&
        playlist.lineSequence().any { it.startsWith(SEGMENT) && !it.trimEnd().endsWith(LIVE_TITLE) }

/** The connect or read timeout that keeps a request within [deadline], at most [TIMEOUT_MILLIS], or 0 when it has passed. */
private fun timeoutBefore(deadline: Long): Int = ((deadline - System.currentTimeMillis()) / 2).coerceIn(0, TIMEOUT_MILLIS.toLong()).toInt()

private fun fetchToken(channel: String, platform: String, playerType: String, deadline: Long): Token? {
    val timeout = timeoutBefore(deadline).takeIf { it > 0 } ?: return null
    val params = JSONObject().put("platform", platform).put("playerBackend", "mediaplayer").put("playerType", playerType)
    val request = JSONObject().put("query", TOKEN_QUERY).put("variables", JSONObject().put("login", channel).put("params", params))
    return try {
        openHttp(GQL_URL) {
            connectTimeout = timeout
            readTimeout = timeout
            requestMethod = "POST"
            setRequestProperty("Client-ID", AdsSettings.swapEmbeddedAdsClientId.value)
            setRequestProperty("X-Device-Id", deviceId)
            setRequestProperty("Content-Type", "application/json")
            send(request.toString().toByteArray(Charsets.UTF_8))
            if (responseCode != HttpURLConnection.HTTP_OK) return@openHttp null
            val token = JSONObject(inputStream.bufferedReader().use { it.readText() })
                .optJSONObject("data")
                ?.optJSONObject("streamPlaybackAccessToken") ?: return@openHttp null
            val value = token.optString("value")
            val signature = token.optString("signature")
            // Usher refuses a forbidden token, such as one for a sub-only or geoblocked stream, so its playlist would only use up search time.
            val forbidden = token.optJSONObject("authorization")?.optBoolean("isForbidden") == true
            if (value.isEmpty() || signature.isEmpty() || forbidden) null else Token(value, signature)
        }
    } catch (_: IOException) {
        null
    } catch (_: JSONException) {
        null
    }
}

private fun fetchText(url: String, deadline: Long): String? {
    val timeout = timeoutBefore(deadline).takeIf { it > 0 } ?: return null
    return try {
        openHttp(url) {
            connectTimeout = timeout
            readTimeout = timeout
            if (responseCode == HttpURLConnection.HTTP_OK) inputStream.bufferedReader().use { it.readText() } else null
        }
    } catch (_: IOException) {
        null
    }
}
