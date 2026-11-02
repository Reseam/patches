// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import app.reseam.bundle.Logger
import app.reseam.youtube.core.debug
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal data class StreamConfiguration(
    val clients: List<ClientType>,
    val preferAvc: Boolean,
    val locale: Locale?,
)

internal object StreamingDataRequest {
    const val VISITOR_ID_HEADER = "X-Goog-Visitor-Id"
    const val REQUEST_TIMEOUT_MILLISECONDS = 30_000L
    private const val MAX_CACHED_REQUESTS = 50
    // Not auto-dubbed, so a client that returns one audio track returns the original.
    private val originalAudioLocale = Locale.forLanguageTag("nb")
    val executor =
        ThreadPoolExecutor(
            4,
            4,
            0,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue<Runnable>(MAX_CACHED_REQUESTS),
        )
    val deadlines = ScheduledThreadPoolExecutor(1).apply { removeOnCancelPolicy = true }
    private val requests = linkedMapOf<String, StreamRequest>()
    private var configuration = StreamConfiguration(ClientType.entries.toList(), false, null)
    private var requestHeaders: Map<String, String> = emptyMap()
    private var offlineHeaders: Map<String, String> = emptyMap()
    var generation = 0L
        private set

    @Volatile var authorization: String? = null

    @Synchronized
    fun setClientOrder(
        preferred: ClientType,
        available: List<ClientType>,
        preferMultipleAvc: Boolean,
        forceOriginalAudio: Boolean,
    ) {
        val order = (listOf(preferred) + available).distinct()
        val updated =
            StreamConfiguration(
                order,
                preferMultipleAvc,
                if (forceOriginalAudio) originalAudioLocale else null,
            )
        if (updated == configuration) return
        clearRequests()
        configuration = updated
        Logger.debug { "Available spoof clients: $order" }
    }

    @Synchronized fun usesWebPlayer(): Boolean = configuration.clients.any { it.usesWebPlayer }

    val clientOsName: String?
        @Synchronized get() = configuration.clients[0].osName

    // Player requests can bypass the hooked HTTP stack, so the visitor identity comes from any InnerTube request.
    @Synchronized
    fun setIdentityHeaders(innertubeHeaders: Map<String?, String?>?) {
        val headers = forwardedHeaders(innertubeHeaders)
        val visitor = headers[VISITOR_ID_HEADER] ?: return
        val previous = requestHeaders[VISITOR_ID_HEADER]
        if (visitor == previous) return
        if (previous != null) clearRequests()
        requestHeaders = headers
    }

    @Synchronized
    fun setOfflineHeaders(playerHeaders: Map<String?, String?>?) {
        offlineHeaders = forwardedHeaders(playerHeaders)
    }

    @Synchronized
    fun fetchRequest(
        videoId: String,
        playerHeaders: Map<String?, String?>?,
        playerBody: ByteArray?,
    ) {
        val headers = forwardedHeaders(playerHeaders)
        if (requestHeaders != headers) {
            clearRequests()
            requestHeaders = headers
        }
        requests.values.removeAll { request ->
            val expired = request.isExpired
            if (expired) request.cancel()
            expired
        }
        val reloadToken = reloadToken(playerBody)
        if (reloadToken != null)
            Logger.debug { "Forwarding native SABR reload context for $videoId" }
        startRequest(videoId, headers, reloadToken)
    }

    // Called under the same lock as configuration changes and on-demand cache lookups.
    private fun startRequest(
        videoId: String,
        headers: Map<String, String>,
        reloadToken: String?,
    ): StreamRequest {
        val previous = requests[videoId]
        if (
            previous != null &&
                !previous.task.isDone &&
                previous.headers == headers &&
                previous.reloadToken == reloadToken
        ) {
            return previous
        }
        if (previous != null) {
            requests.remove(videoId)
            previous.cancel()
        }
        if (requests.size >= MAX_CACHED_REQUESTS) requests.remove(requests.keys.first())?.cancel()
        val request = StreamRequest(videoId, headers, configuration, generation, reloadToken, false)
        requests[videoId] = request
        request.timeout =
            deadlines.schedule(request::cancel, REQUEST_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
        try {
            executor.execute(request.task)
        } catch (exception: RuntimeException) {
            request.cancel()
            requests.remove(videoId, request)
            Logger.error(exception) { "Could not schedule stream request" }
        }
        return request
    }

    @Synchronized
    fun requestFor(videoId: String): StreamRequest {
        val cached = requests[videoId]
        return cached?.takeUnless { it.isExpired }
            ?: startRequest(videoId, requestHeaders, cached?.reloadToken)
    }

    @Synchronized
    fun offlineRequest(videoId: String): StreamRequest =
        StreamRequest(
            videoId,
            offlineHeaders.ifEmpty { requestHeaders },
            configuration,
            generation,
            null,
            true,
        )

    @Synchronized
    fun discard(request: StreamRequest) {
        requests.remove(request.videoId, request)
    }

    private fun clearRequests() {
        generation++
        requests.values.forEach(StreamRequest::cancel)
        requests.clear()
    }
}
