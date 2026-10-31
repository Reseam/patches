// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.spoof

import android.os.SystemClock
import app.reseam.bundle.Logger
import app.reseam.bundle.isMainThread
import java.util.concurrent.TimeUnit

internal fun awaitStreams(videoId: String): StreamingData? {
    if (isMainThread()) {
        Logger.error { "Cannot await spoof streams on the main thread" }
        return null
    }
    val request = StreamingDataRequest.requestFor(videoId)
    try {
        // Completed FutureTasks remain readable after the fetch deadline; URLs have their own
        // lifetime.
        val wait = maxOf(0, request.deadline - SystemClock.elapsedRealtime())
        val result = request.task.get(wait, TimeUnit.MILLISECONDS)
        synchronized(StreamingDataRequest) {
            if (request.generation != StreamingDataRequest.generation) return null
            if (result != null && result.isFresh) return result
        }
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        return null
    } catch (exception: Exception) {
        Logger.error(exception) { "Spoof stream response failed for $videoId" }
    }
    StreamingDataRequest.discard(request)
    request.cancel()
    return null
}

internal fun fetchOfflineStreams(videoId: String): StreamingData? {
    if (isMainThread()) {
        Logger.error { "Cannot fetch offline streams on the main thread" }
        return null
    }
    val request = StreamingDataRequest.offlineRequest(videoId)
    request.timeout =
        StreamingDataRequest.deadlines.schedule(
            request::cancel,
            StreamingDataRequest.REQUEST_TIMEOUT_MILLISECONDS,
            TimeUnit.MILLISECONDS,
        )
    request.task.run()
    try {
        return request.task.get()
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
    } catch (exception: Exception) {
        Logger.error(exception) { "Offline stream request failed for $videoId" }
    }
    return null
}
