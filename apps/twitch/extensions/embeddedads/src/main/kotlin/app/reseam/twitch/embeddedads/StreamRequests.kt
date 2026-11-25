// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.twitch.embeddedads

import android.net.Uri
import app.reseam.bundle.openHttp
import app.reseam.bundle.send
import app.reseam.twitch.settings.AdsSettings
import java.io.IOException
import java.net.HttpURLConnection
import java.util.UUID
import org.json.JSONException
import org.json.JSONObject

private const val GQL_URL = "https://gql.twitch.tv/gql"
private const val TOKEN_QUERY =
    $$"query($login: String!, $params: PlaybackAccessTokenParams!) { streamPlaybackAccessToken(channelName: $login, params: $params) { value signature authorization { isForbidden } } }"
private const val TIMEOUT_MILLIS = 1_500

// A token asked for without a device ID can start on a "commercial break in progress" slate.
private val deviceId = UUID.randomUUID().toString().replace("-", "")

internal class Token(val value: String, val signature: String)

internal fun fetchToken(channel: String, platform: String, playerType: String, deadline: Long): Token? {
    val params = JSONObject().put("platform", platform).put("playerBackend", "mediaplayer").put("playerType", playerType)
    val request = JSONObject().put("query", TOKEN_QUERY).put("variables", JSONObject().put("login", channel).put("params", params))
    val response = fetchText(GQL_URL, deadline) {
        requestMethod = "POST"
        setRequestProperty("Client-ID", AdsSettings.swapEmbeddedAdsClientId.value)
        setRequestProperty("X-Device-Id", deviceId)
        setRequestProperty("Content-Type", "application/json")
        send(request.toString().toByteArray(Charsets.UTF_8))
    } ?: return null
    val token = try {
        JSONObject(response).optJSONObject("data")?.optJSONObject("streamPlaybackAccessToken")
    } catch (_: JSONException) {
        null
    } ?: return null
    val value = token.optString("value")
    val signature = token.optString("signature")
    // Usher refuses a forbidden token, such as one for a sub-only or geoblocked stream, so its playlist would only use up search time.
    val forbidden = token.optJSONObject("authorization")?.optBoolean("isForbidden") == true
    return if (value.isEmpty() || signature.isEmpty() || forbidden) null else Token(value, signature)
}

/** [usherUrl] with [token] in place of the token it carries. */
internal fun usherUrlWith(usherUrl: String, token: Token): String {
    val original = Uri.parse(usherUrl)
    val builder = original.buildUpon().clearQuery()
    for (name in original.queryParameterNames) {
        if (name == "token" || name == "sig") continue
        original.getQueryParameters(name).forEach { builder.appendQueryParameter(name, it) }
    }
    return builder.appendQueryParameter("token", token.value).appendQueryParameter("sig", token.signature).build().toString()
}

/** The body of a successful request to [url] that ends before [deadline], or null. */
internal fun fetchText(url: String, deadline: Long, prepare: HttpURLConnection.() -> Unit = {}): String? {
    // Half the time left each for connecting and reading keeps the request within the deadline.
    val timeout = ((deadline - System.currentTimeMillis()) / 2).coerceAtMost(TIMEOUT_MILLIS.toLong()).toInt()
    if (timeout <= 0) return null
    return try {
        openHttp(url) {
            connectTimeout = timeout
            readTimeout = timeout
            prepare()
            if (responseCode == HttpURLConnection.HTTP_OK) inputStream.bufferedReader().use { it.readText() } else null
        }
    } catch (_: IOException) {
        null
    }
}
