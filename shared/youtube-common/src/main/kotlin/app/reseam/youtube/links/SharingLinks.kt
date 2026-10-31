// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("SharingLinks")

package app.reseam.youtube.links

import android.content.Intent
import android.net.Uri
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug

// "is" appears alongside "si" on some accounts; "feature" is the older tracking parameter.
private val trackingParameters = setOf("si", "is", "feature")

@Export
fun sanitizeExtra(key: String?, value: String?): String? =
    if (key == Intent.EXTRA_TEXT) sanitize(value) else value

private fun isYouTube(host: String?): Boolean =
    when (host) {
        null -> false
        "youtu.be",
        "youtube.com" -> true
        else -> host.endsWith(".youtube.com")
    }

@Export
fun sanitize(url: String?): String? {
    if (url == null) return null
    return try {
        val uri = Uri.parse(url)
        // The share sheet also passes video titles, which are not URLs.
        if (uri.scheme !in listOf("https", "http")) return url
        // Other sites may use these parameter names for content.
        if (!isYouTube(uri.host)) return url
        val builder = uri.buildUpon().clearQuery()
        uri.queryParameterNames
            .filterNot { it in trackingParameters }
            .forEach { name ->
                uri.getQueryParameters(name).forEach { value ->
                    builder.appendQueryParameter(name, value)
                }
            }
        builder.build().toString().also { sanitized ->
            Logger.debug { "Sanitized $url to $sanitized" }
        }
    } catch (ex: Exception) {
        Logger.error(ex) { "Could not sanitize $url" }
        url
    }
}
