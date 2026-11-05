// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.withheld

import android.net.Uri
import app.reseam.bundle.openHttp
import java.net.HttpURLConnection
import org.json.JSONObject

// Protected accounts are refused even when a response carries them.
internal object FxTwitter {
    private const val API = "https://api.fxtwitter.com/2"
    private const val TIMEOUT_MS = 8_000

    fun status(id: String): FxStatus? =
        get("status/$id")?.optJSONObject("status")?.toFxStatus()?.takeIf { it.id == id && !it.author.isProtected }

    fun profile(userId: String): FxAuthor? =
        get("profile/id:$userId")?.optJSONObject("user")?.toFxAuthor()?.takeIf { it.id == userId && !it.isProtected }

    fun statuses(userId: String, cursor: String?, withReplies: Boolean): FxPage? =
        get(
            "profile/id:$userId/statuses",
            "count" to "20",
            "cursor" to cursor,
            "with_replies" to "1".takeIf { withReplies },
        )?.toFxPage()?.let { page -> page.copy(statuses = page.statuses.filterNot { it.author.isProtected }) }

    private fun get(path: String, vararg query: Pair<String, String?>): JSONObject? {
        val builder = Uri.parse("$API/$path").buildUpon()
        query.forEach { (key, value) -> value?.let { builder.appendQueryParameter(key, it) } }
        return openHttp(builder.build().toString()) {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", "Reseam/X")
            if (responseCode != HttpURLConnection.HTTP_OK) return@openHttp null
            JSONObject(inputStream.bufferedReader().use { it.readText() })
        }
    }
}
