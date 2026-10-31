// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import app.reseam.bundle.openHttp
import app.reseam.bundle.send
import app.reseam.youtube.core.readBounded
import app.reseam.youtube.web.WebPlayer
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal class StreamRequestHttp(
    private val headers: Map<String, String>,
    private val offline: Boolean,
) {
    @Volatile private var connection: HttpURLConnection? = null

    fun cancel() {
        connection?.disconnect()
    }

    fun send(
        client: ClientType,
        json: String,
        attestation: WebPlayer.Attestation?,
        attemptDeadline: Long,
    ): ByteArray {
        val body = json.toByteArray(Charsets.UTF_8)
        var attemptTimeout: ScheduledFuture<*>? = null
        val url = if (client == ClientType.TV || offline) FULL_PLAYER_URL else PLAYER_URL
        return try {
            openHttp(url) {
                connection = this
                try {
                    val budget = remaining(attemptDeadline)
                    attemptTimeout =
                        StreamingDataRequest.deadlines.schedule(
                            this::disconnect,
                            budget.toLong(),
                            TimeUnit.MILLISECONDS,
                        )
                    connectTimeout = minOf(HTTP_TIMEOUT_MILLISECONDS, budget)
                    readTimeout = minOf(HTTP_TIMEOUT_MILLISECONDS, budget)
                    headers.forEach { (name, value) -> setRequestProperty(name, value) }
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", client.userAgent)
                    if (client == ClientType.TV)
                        setRequestProperty(
                            "Authorization",
                            checkNotNull(StreamingDataRequest.authorization) {
                                "Account signed out"
                            },
                        )
                    setRequestProperty("X-YouTube-Client-Name", client.clientId.toString())
                    setRequestProperty(
                        "X-YouTube-Client-Version",
                        attestation?.client?.getString("clientVersion") ?: client.clientVersion,
                    )
                    val visitorData = attestation?.client?.optString("visitorData") ?: ""
                    if (visitorData.isNotEmpty())
                        setRequestProperty(StreamingDataRequest.VISITOR_ID_HEADER, visitorData)
                    requestMethod = "POST"
                    useCaches = false
                    send(body)
                    remaining(attemptDeadline)
                    val status = responseCode
                    if (status != HttpURLConnection.HTTP_OK)
                        throw IOException("Player HTTP $status")
                    inputStream.use { input ->
                        input.readBounded(
                            MAX_RESPONSE_BYTES,
                            8192,
                            "Player response is too large",
                        ) {
                            remaining(attemptDeadline)
                        }
                    }
                } finally {
                    attemptTimeout?.cancel(false)
                }
            }
        } finally {
            connection = null
        }
    }
}

private const val PLAYER_URL =
    "https://youtubei.googleapis.com/youtubei/v1/player?fields=playabilityStatus,streamingData&alt=proto"
private const val FULL_PLAYER_URL = "https://youtubei.googleapis.com/youtubei/v1/player?alt=proto"
private const val HTTP_TIMEOUT_MILLISECONDS = 10_000
internal const val MAX_RESPONSE_BYTES = 8 * 1024 * 1024
