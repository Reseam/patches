// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import android.os.SystemClock
import app.reseam.bundle.openHttp
import app.reseam.youtube.core.readResponseText
import java.io.IOException
import java.net.HttpURLConnection
import kotlin.time.Duration.Companion.hours
import org.json.JSONObject
import org.json.JSONTokener

internal object TvProfile {
    // The Wii U profile is served without the attestation expected from most TV devices.
    const val USER_AGENT =
        "Mozilla/5.0 (Nintendo WiiU) AppleWebKit/536.30 (KHTML, like Gecko) " +
            "NX/3.0.4.2.12 NintendoBrowser/4.3.1.11264.US"
    private const val PAGE_URL = "https://www.youtube.com/tv"
    private const val CONFIG_CALL = "ytcfg.set("
    private val maxAgeMilliseconds = 12.hours.inWholeMilliseconds
    private const val MAX_PAGE_BYTES = 2 * 1024 * 1024
    private val identity =
        listOf(
            "clientName",
            "clientVersion",
            "deviceMake",
            "deviceModel",
            "osName",
            "osVersion",
            "platform",
            "userAgent",
        )

    private data class Profile(val client: JSONObject, val loadedAt: Long)

    private var profile: Profile? = null

    @Synchronized
    fun get(timeout: Int): JSONObject {
        val cached =
            profile?.takeIf { SystemClock.elapsedRealtime() - it.loadedAt < maxAgeMilliseconds }
                ?: Profile(load(timeout), SystemClock.elapsedRealtime()).also { profile = it }
        return JSONObject(cached.client.toString())
    }

    private fun load(timeout: Int): JSONObject {
        val page =
            openHttp(PAGE_URL) {
                requestMethod = "GET"
                connectTimeout = timeout
                readTimeout = timeout
                useCaches = false
                setRequestProperty("User-Agent", USER_AGENT)
                val status = responseCode
                if (status != HttpURLConnection.HTTP_OK) throw IOException("TV page HTTP $status")
                readResponseText(MAX_PAGE_BYTES, "TV page is too large", bufferSize = 8192)
            }
        val start = page.indexOf(CONFIG_CALL)
        if (start < 0) throw IOException("TV page has no configuration")
        val config =
            JSONTokener(page.substring(start + CONFIG_CALL.length)).nextValue() as JSONObject
        val loaded = config.getJSONObject("INNERTUBE_CONTEXT").getJSONObject("client")
        if (
            loaded.optString("clientName") != ClientType.TV.clientName ||
                loaded.optString("clientVersion").isEmpty()
        ) {
            throw IOException("TV page has no TV client context")
        }
        return JSONObject().apply { identity.forEach { put(it, loaded.opt(it)) } }
    }
}
