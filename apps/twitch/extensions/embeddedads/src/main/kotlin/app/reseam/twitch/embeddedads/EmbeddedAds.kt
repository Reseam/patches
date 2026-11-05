// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("EmbeddedAds")

package app.reseam.twitch.embeddedads

import app.reseam.runtime.Export
import app.reseam.twitch.settings.AdsSettings

private const val LIVE_PLAYLIST_PREFIX = "https://usher.ttvnw.net/api/"
private const val CHANNEL_PATH = "/channel/hls/"
private const val PLAYLIST_EXTENSION = ".m3u8"

// The proxy reads the playlist query from its path.
private const val PROXY_QUERY = "%3Fallow_source%3Dtrue%26allow_audio_only%3Dtrue%26fast_bread%3Dtrue"
private const val FALLBACK_MILLIS = 5 * 60 * 1000L
private const val NOT_FOUND = 404

// Playlists load from Twitch until then, so the player's retry after a proxy failure succeeds.
@Volatile
private var proxyPausedUntil = 0L

@Export
fun playlistUrl(url: String?): String? {
    if (url == null) return null
    val channelStart = proxiedChannelStart(url)
    if (channelStart < 0) return url
    val channel = url.substring(channelStart, url.indexOf(PLAYLIST_EXTENSION, channelStart))
    return playlistPrefix() + channel + PLAYLIST_EXTENSION + PROXY_QUERY
}

// The proxy gets none of the player's headers, so none of its tokens.
@Export
fun playlistHeaders(url: String?, headers: Map<String, String>?): Map<String, String>? =
    if (url != null && proxiedChannelStart(url) >= 0) emptyMap() else headers

@Export
fun onResponse(status: Int, url: String?) {
    if (status >= 400 && status != NOT_FOUND && isProxied(url)) pauseProxy()
}

@Export
fun onFailure(url: String?) {
    if (isProxied(url)) pauseProxy()
}

/** The channel's start in a live playlist URL the proxy should serve, or -1. */
private fun proxiedChannelStart(url: String): Int {
    if (!url.startsWith(LIVE_PLAYLIST_PREFIX) || System.currentTimeMillis() < proxyPausedUntil) return -1
    val channelPath = url.indexOf(CHANNEL_PATH, LIVE_PLAYLIST_PREFIX.length - 1)
    if (channelPath < 0) return -1
    val channelStart = channelPath + CHANNEL_PATH.length
    return if (url.indexOf(PLAYLIST_EXTENSION, channelStart) > channelStart) channelStart else -1
}

// A choice always reads as one of its listed base URLs, so the value needs no checks.
private fun playlistPrefix(): String = AdsSettings.embeddedAdsProxy.value + "/playlist/"

private fun isProxied(url: String?): Boolean = url != null && url.startsWith(playlistPrefix())

private fun pauseProxy() {
    proxyPausedUntil = System.currentTimeMillis() + FALLBACK_MILLIS
}
