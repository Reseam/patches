// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("TokenServer")

package app.reseam.youtube.web

import android.os.SystemClock
import android.util.Base64
import app.reseam.bundle.openHttp
import app.reseam.bundle.send
import app.reseam.youtube.core.TextResponse
import app.reseam.youtube.core.YouTubeContext
import app.reseam.youtube.core.readResponseText
import app.reseam.youtubespoof.settings.SpoofSettings
import java.io.IOException
import java.net.HttpURLConnection
import java.time.Instant
import java.util.concurrent.TimeoutException
import org.json.JSONObject

/**
 * The token API is provided for Reseam-patched apps under https://mint.reseam.app's terms. Other
 * projects may not integrate it, impersonate Reseam, relay or resell tokens without prior
 * permission (contact@reseam.app).
 */
private const val HOST = "https://mint.reseam.app/"
private const val API = "${HOST}v1/"
private const val HTTP_TIMEOUT_MILLISECONDS = 10_000
private const val MAX_RESPONSE_BYTES = 64 * 1024

internal data class ServerToken(val poToken: String, val expiresAt: Long)

// The native integrity binding is an arbitrary byte string, so it is sent base64url-encoded.
internal fun mintAndroidToken(binding: ByteArray, deadline: Long): String {
    val encoded = Base64.encodeToString(binding, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    val body = JSONObject().put("binding", encoded).put("vm", "droidguard").toString()
    val initial = post("tokens", body, credential(deadline), deadline)
    val response =
        if (initial.status == HttpURLConnection.HTTP_UNAUTHORIZED) {
            SpoofSettings.tokenServerCredential.reset()
            post("tokens", body, credential(deadline), deadline)
        } else initial
    return success(response).getString("poToken")
}

internal fun mintServerToken(binding: String, deadline: Long): ServerToken {
    val body = JSONObject().put("binding", binding).toString()
    val initial = post("tokens", body, credential(deadline), deadline)
    val response =
        if (initial.status == HttpURLConnection.HTTP_UNAUTHORIZED) {
            SpoofSettings.tokenServerCredential.reset()
            post("tokens", body, credential(deadline), deadline)
        } else initial
    val token = success(response)
    val lifetime =
        Instant.parse(token.getString("expiresAt")).toEpochMilli() - System.currentTimeMillis()
    return ServerToken(token.getString("poToken"), SystemClock.elapsedRealtime() + lifetime)
}

@Synchronized
private fun credential(deadline: Long): String {
    val saved = SpoofSettings.tokenServerCredential.value
    if (saved.isNotEmpty()) return saved
    val body =
        JSONObject().put("packageName", YouTubeContext.applicationContext.packageName).toString()
    val credential = success(post("installations", body, null, deadline)).getString("credential")
    SpoofSettings.tokenServerCredential.value = credential
    return credential
}

private fun success(response: TextResponse): JSONObject {
    if (response.status / 100 == 2) return response.json()
    throw IOException("Token server HTTP ${response.status}: ${response.body.orEmpty().take(200)}")
}

// The readiness check is public; /readyz answers 503 when BotGuard has no worker, with the same body.
internal fun droidguardReady(deadline: Long): Boolean =
    request("${HOST}readyz", null, null, deadline).json().getInt("droidguard") > 0

private fun post(path: String, body: String, credential: String?, deadline: Long): TextResponse =
    request(API + path, body, credential, deadline)

private fun request(url: String, body: String?, credential: String?, deadline: Long): TextResponse =
    openHttp(url) {
        val timeout =
            minOf(HTTP_TIMEOUT_MILLISECONDS.toLong(), deadline - SystemClock.elapsedRealtime())
                .toInt()
        if (timeout <= 0) throw TimeoutException("Token server request deadline exceeded")
        connectTimeout = timeout
        readTimeout = timeout
        requestMethod = if (body == null) "GET" else "POST"
        useCaches = false
        setRequestProperty("User-Agent", "Reseam/YouTube")
        if (credential != null) setRequestProperty("X-Installation-Token", credential)
        if (body != null) {
            setRequestProperty("Content-Type", "application/json")
            send(body.toByteArray(Charsets.UTF_8))
        }
        val status = responseCode
        TextResponse(
            status,
            readResponseText(
                MAX_RESPONSE_BYTES,
                "Token server response is too large",
                errorBody = status >= 400,
            ),
        )
    }
