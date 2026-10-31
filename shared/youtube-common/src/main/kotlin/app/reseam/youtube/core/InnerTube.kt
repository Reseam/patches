// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.core

import app.reseam.bundle.Logger
import app.reseam.bundle.openHttp
import app.reseam.bundle.postToMainThread
import app.reseam.bundle.send
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.Executors
import org.json.JSONException
import org.json.JSONObject

private const val API = "https://youtubei.googleapis.com/youtubei/v1"
private const val TIMEOUT_MILLISECONDS = 5_000

// The app's own client needs its attestation to browse; any WEB release answers public lookups,
// and old ones keep working for years.
private const val CLIENT = """{"client":{"clientName":"WEB","clientVersion":"2.20250101.00.00"}}"""

/** Public data the app does not hand to patches. Results arrive on the main thread, null on failure. */
object InnerTube {
    private val executor = Executors.newSingleThreadExecutor()

    fun channelId(videoId: String, result: (String?) -> Unit) =
        lookup(result) {
            request("player", "videoDetails.channelId", JSONObject().put("videoId", videoId))
                .optJSONObject("videoDetails")
                ?.optString("channelId")
        }

    /** The channel's handle with its @. */
    fun channelHandle(channelId: String, result: (String?) -> Unit) =
        lookup(result) {
            request(
                    "browse",
                    "metadata.channelMetadataRenderer.vanityChannelUrl",
                    JSONObject().put("browseId", channelId),
                )
                .optJSONObject("metadata")
                ?.optJSONObject("channelMetadataRenderer")
                ?.optString("vanityChannelUrl")
                ?.substringAfterLast('/')
                ?.takeIf { it.startsWith("@") }
        }

    private fun lookup(result: (String?) -> Unit, read: () -> String?) {
        executor.execute {
            val value =
                try {
                    read()?.ifEmpty { null }
                } catch (e: IOException) {
                    Logger.warn(e) { "InnerTube lookup failed" }
                    null
                } catch (e: JSONException) {
                    Logger.warn(e) { "InnerTube lookup returned unreadable JSON" }
                    null
                }
            postToMainThread { result(value) }
        }
    }

    private fun request(endpoint: String, fields: String, body: JSONObject): JSONObject =
        openHttp("$API/$endpoint?prettyPrint=false&fields=$fields") {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MILLISECONDS
            readTimeout = TIMEOUT_MILLISECONDS
            setRequestProperty("Content-Type", "application/json")
            send(body.put("context", JSONObject(CLIENT)).toString().toByteArray())
            if (responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP $responseCode")
            JSONObject(inputStream.bufferedReader().use { it.readText() })
        }
}
