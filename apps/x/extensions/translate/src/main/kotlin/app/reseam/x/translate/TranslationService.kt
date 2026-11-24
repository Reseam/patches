// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.translate

import android.os.SystemClock
import app.reseam.bundle.Logger
import app.reseam.bundle.openHttp
import java.io.IOException
import java.net.HttpURLConnection

private const val TIMEOUT_MS = 8_000
private const val LANGUAGES_RETRY_MS = 5 * 60_000L

/** A service that translates post text. Languages are [engineLanguage] codes. */
internal interface TranslationService {
    val name: String

    /** The most texts, and UTF-8 bytes of them, one request may carry. */
    val maxTexts: Int
    val maxBytes: Int

    /** The languages it translates between, or null while the list is unavailable. */
    fun languages(): Set<String>?

    fun translate(from: String, to: String, texts: List<String>): List<String>
}

/** A service's language list, loaded on first use and loaded again a while after a failure. */
internal class LanguageList(private val service: String, private val load: () -> Set<String>) {
    @Volatile private var languages: Set<String>? = null
    @Volatile private var retryAt = 0L

    fun get(): Set<String>? {
        languages?.let { return it }
        if (SystemClock.elapsedRealtime() < retryAt) return null
        return runCatching(load)
            .onSuccess { languages = it }
            .onFailure {
                retryAt = SystemClock.elapsedRealtime() + LANGUAGES_RETRY_MS
                Logger.warn(it) { "$service languages are unavailable" }
            }
            .getOrNull()
    }
}

/** The body of a successful request to [url]. */
internal inline fun TranslationService.fetch(url: String, configure: HttpURLConnection.() -> Unit = {}): String =
    openHttp(url) {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        setRequestProperty("User-Agent", "Reseam/X")
        configure()
        if (responseCode != HttpURLConnection.HTTP_OK) throw IOException("$name HTTP $responseCode")
        inputStream.bufferedReader().use { it.readText() }
    }
