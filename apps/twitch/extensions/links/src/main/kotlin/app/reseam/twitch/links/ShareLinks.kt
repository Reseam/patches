// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ShareLinks")

package app.reseam.twitch.links

import android.net.Uri
import app.reseam.runtime.Export
import java.util.Locale

private const val TWITCH_HOST = "twitch.tv"

// A share text can end a sentence right after the link.
private const val TRAILING_PUNCTUATION = ".,!?;:)]'"

// Twitch reads its campaign keys under both the tt_ and the utm_ prefix.
private val trackingNames = setOf("sr", "referrer_url")
private val trackingPrefixes = listOf("tt_", "utm_")

// The Japanese and Korean share texts put the next word right after the link.
private val link = Regex("""https?://[A-Za-z0-9\-._~:/?#\[\]@!$&'()*+,;=%]+""", RegexOption.IGNORE_CASE)

/** Returns [text] without the attribution and campaign parameters of every twitch.tv link in it. */
@Export
fun clean(text: String?): String? {
    if (text == null || !text.contains(TWITCH_HOST, ignoreCase = true)) return text
    return link.replace(text) { match ->
        val url = match.value.trimEnd { it in TRAILING_PUNCTUATION }
        cleanUrl(url) + match.value.substring(url.length)
    }
}

private fun cleanUrl(url: String): String {
    val uri = Uri.parse(url)
    val host = uri.host?.lowercase(Locale.ROOT)
    val query = uri.encodedQuery
    if (host == null || query == null || (host != TWITCH_HOST && !host.endsWith(".$TWITCH_HOST"))) return url
    val parameters = query.split('&')
    val kept = parameters.filterNot { isTracking(it.substringBefore('=').lowercase(Locale.ROOT)) }
    if (kept.size == parameters.size) return url
    return uri.buildUpon().encodedQuery(kept.joinToString("&").ifEmpty { null }).build().toString()
}

private fun isTracking(name: String) = name in trackingNames || trackingPrefixes.any { name.startsWith(it) }
