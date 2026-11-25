// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("VideoSwap")

package app.reseam.twitch.embeddedads

import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.twitch.settings.AdsSettings
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

private const val KEPT_STREAMS = 16

private class Body(val url: String) {
    var bytes: ByteArray? = null
    var offset = 0
}

private val streamsByVariantUrl = ConcurrentHashMap<String, LiveStream>()
private val streamsByUsherUrl = LinkedHashMap<String, LiveStream>(KEPT_STREAMS, 0.75f, true)
private val bodiesByProvider = Collections.synchronizedMap(WeakHashMap<Any, Body>())

/** Marks the body that [provider] reads as a live playlist when [url] is one. */
@Export
fun onBody(provider: Any?, url: String?) {
    if (provider == null || url == null || AdsSettings.embeddedAdsMethod.value != "swap") return
    if (streamsByVariantUrl.containsKey(url) || channelOf(url) != null) bodiesByProvider[provider] = Body(url)
}

@Export
fun reads(provider: Any?): Boolean = provider != null && bodiesByProvider.containsKey(provider)

/** Fills [buffer] from the playlist that [provider] reads from [source], and returns the byte count or -1 at the end. */
@Export
fun writeTo(provider: Any?, source: ReadableByteChannel?, buffer: ByteBuffer?): Int {
    val body = provider?.let { bodiesByProvider[it] } ?: return -1
    val bytes = body.bytes ?: playlistOrOriginal(body.url, Channels.newInputStream(source).readBytes()).also { body.bytes = it }
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
        Logger.warn(error) { "Embedded ads: kept a playlist as Twitch sent it" }
        original
    }

private fun playlistFor(url: String, original: ByteArray): ByteArray {
    val channel = channelOf(url)
    if (channel != null) {
        track(url, channel, original)
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
