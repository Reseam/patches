// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.web

import android.os.SystemClock
import app.reseam.bundle.Logger
import app.reseam.youtube.core.YouTubeContext
import org.json.JSONArray

internal class WebGeneration {
    val createdAt = SystemClock.elapsedRealtime()
    val runtime = WebRuntime(YouTubeContext.applicationContext)
    val prepared =
        runtime.call("web.prepare", JSONArray().put(true)).thenApply { it.toInt() }
    var users = 0
    var retired = false

    init {
        prepared.whenComplete { _, error ->
            if (error != null) {
                runtime.close(error)
                Logger.error(error) { "Web player preparation failed" }
            }
        }
    }

    fun closeIfUnused() {
        if (retired && users == 0) runtime.close(IllegalStateException("Web player refreshed"))
    }
}
