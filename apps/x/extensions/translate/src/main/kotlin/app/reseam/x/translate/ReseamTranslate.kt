// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

import android.os.SystemClock
import app.reseam.bundle.Logger
import app.reseam.bundle.openHttp
import app.reseam.bundle.send
import app.reseam.x.api.strings
import java.io.IOException
import java.net.HttpURLConnection
import org.json.JSONArray
import org.json.JSONObject

internal object ReseamTranslate {
    private const val API = "https://api.reseam.app/v1/translate"
    private const val TIMEOUT_MS = 8_000
    private const val LANGUAGES_RETRY_MS = 5 * 60_000L

    @Volatile private var languages: Set<String>? = null
    @Volatile private var languagesRetryAt = 0L

    /** The languages it translates between, or null while the list is unavailable. */
    fun languages(): Set<String>? = languages ?: loadLanguages()

    fun translate(from: String, to: String, texts: List<String>): List<String> {
        val body = JSONObject().put("from", from).put("to", to).put("texts", JSONArray(texts))
        val response = request(API) {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            send(body.toString().toByteArray())
        }
        val translations = response.getJSONArray("translations").strings()
        return translations.takeIf { it.size == texts.size }
            ?: throw IOException("Got ${translations.size} translations for ${texts.size} texts")
    }

    private fun loadLanguages(): Set<String>? {
        if (SystemClock.elapsedRealtime() < languagesRetryAt) return null
        return runCatching { request("$API/languages") {}.getJSONArray("pairs") }
            .map { pairs -> (0 until pairs.length()).flatMap { pairs.getJSONArray(it).strings() }.toSet() }
            .onSuccess { languages = it }
            .onFailure {
                languagesRetryAt = SystemClock.elapsedRealtime() + LANGUAGES_RETRY_MS
                Logger.warn(it) { "Reseam Translate languages are unavailable" }
            }
            .getOrNull()
    }

    private inline fun request(url: String, configure: HttpURLConnection.() -> Unit): JSONObject =
        openHttp(url) {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", "Reseam/X")
            configure()
            if (responseCode != HttpURLConnection.HTTP_OK) throw IOException("Reseam Translate HTTP $responseCode")
            JSONObject(inputStream.bufferedReader().use { it.readText() })
        }
}
