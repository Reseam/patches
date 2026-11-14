// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.crowdcache

import app.reseam.bundle.jsonObject
import app.reseam.bundle.openHttp
import app.reseam.bundle.send
import java.io.IOException
import java.net.HttpURLConnection
import java.security.MessageDigest
import org.json.JSONObject

/** Reseam's shared store of results from outside services: what one person computed, everyone reuses. */
object Crowdcache {
    private const val API = "https://crowdcache.reseam.app/v1"
    private const val TIMEOUT_MS = 8_000
    internal const val MAX_LOOKUP = 100

    internal fun lookup(namespace: String, keys: Collection<String>): Map<String, JSONObject> {
        val entries = JSONObject(post("$API/$namespace/lookup", jsonObject("keys" to keys.toList()))).getJSONObject("entries")
        return entries.keys().asSequence().associateWith { entries.getJSONObject(it) }
    }

    fun submit(namespace: String, key: String, value: JSONObject) {
        post("$API/$namespace", jsonObject("entries" to listOf(jsonObject("key" to key, "value" to value))))
    }

    private fun post(url: String, body: JSONObject): String =
        openHttp(url) {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "Reseam")
            send(body.toString().toByteArray())
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_NO_CONTENT -> ""
                else -> throw IOException("Crowdcache HTTP $responseCode")
            }
        }
}

fun sha256(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
