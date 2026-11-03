// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("EmbeddedAds")

package app.reseam.twitch.embeddedads

import app.reseam.runtime.Export

private const val LIVE_PLAYLIST_PREFIX = "https://usher.ttvnw.net/api/"
private const val CHANNEL_PATH = "/channel/hls/"
private const val PLAYLIST_EXTENSION = ".m3u8"
// The proxy reads the playlist query encoded into its path
private const val PROXY_QUERY = "%3Fallow_source%3Dtrue%26allow_audio_only%3Dtrue%26fast_bread%3Dtrue"
private const val FALLBACK_MILLISECONDS = 5 * 60 * 1000L
private const val NOT_FOUND = 404
private const val DEFAULT_PROXY = "https://eu.luminous.dev"
// An https base URL without a query, the form the proxy setting asks for
private val proxyUrlPattern = Regex("https://[A-Za-z0-9.-]+(:[0-9]+)?(/[A-Za-z0-9._~/-]*)?")

// Playlists load from Twitch until this time, so the player's retry after a proxy failure succeeds
@Volatile
private var proxyPausedUntil = 0L

// The patch replaces this body with a read of the proxy setting
@Export
fun proxyUrl(): String = ""

// The class loader builds the prefix on first use, so later requests take no lock
private object Proxy {
    val playlistPrefix = proxyBase() + "/playlist/"
}

// The proxy setting without trailing slashes, or the default proxy when the setting is not a valid base URL
private fun proxyBase(): String {
    val proxy = proxyUrl().trim().trimEnd('/')
    return if (proxyUrlPattern.matches(proxy)) proxy else DEFAULT_PROXY
}

@Suppress("unused")
@Export
fun playlistUrl(url: String?): String? {
    if (url == null) return null
    val channelStart = proxiedChannelStart(url)
    if (channelStart < 0) return url

    val channel = url.substring(channelStart, url.indexOf(PLAYLIST_EXTENSION, channelStart))
    return Proxy.playlistPrefix + channel + PLAYLIST_EXTENSION + PROXY_QUERY
}

@Suppress("unused")
@Export
fun playlistHeaders(url: String?, headers: Map<String, String>?): Map<String, String>? {
    if (url == null || proxiedChannelStart(url) < 0) return headers
    return emptyMap()
}

@Suppress("unused")
@Export
fun onResponse(status: Int, url: String?) {
    if (status >= 400 && status != NOT_FOUND && isProxied(url)) pauseProxy()
}

@Suppress("unused")
@Export
fun onFailure(url: String?) {
    if (isProxied(url)) pauseProxy()
}

// Start of the channel name in a live playlist URL the proxy should serve, or a negative index to leave the URL alone
private fun proxiedChannelStart(url: String): Int {
    if (!url.startsWith(LIVE_PLAYLIST_PREFIX)) return -1
    val channelPath = url.indexOf(CHANNEL_PATH, LIVE_PLAYLIST_PREFIX.length - 1)
    if (channelPath < 0) return -1
    val channelStart = channelPath + CHANNEL_PATH.length
    if (url.indexOf(PLAYLIST_EXTENSION, channelStart) <= channelStart) return -1
    if (System.currentTimeMillis() < proxyPausedUntil) return -1
    return channelStart
}

private fun isProxied(url: String?): Boolean = url != null && url.startsWith(Proxy.playlistPrefix)

private fun pauseProxy() {
    proxyPausedUntil = System.currentTimeMillis() + FALLBACK_MILLISECONDS
}
