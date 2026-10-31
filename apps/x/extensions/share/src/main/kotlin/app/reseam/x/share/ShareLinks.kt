// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ShareLinks")

package app.reseam.x.share

import app.reseam.bundle.ReseamPatches
import app.reseam.runtime.Export
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

private val xHosts =
    setOf("x.com", "www.x.com", "twitter.com", "www.twitter.com", "mobile.twitter.com")
@Volatile private var providerKey: String? = null

@Export
fun configure(key: String) {
    providerKey = key
}

@Export
fun rewrite(link: String?): String? {
    val key = providerKey ?: return link
    if (link == null) return null
    val provider = ReseamPatches.bundle().settings.getChoice(key)
    if (provider == "off") return link
    val uri =
        try {
            URI(link.trim())
        } catch (_: URISyntaxException) {
            return link
        }
    val scheme = uri.scheme ?: return link
    val host = uri.host ?: return link
    val path = uri.rawPath ?: return link
    val isHttp = listOf("https", "http").any { scheme.equals(it, ignoreCase = true) }
    if (!isHttp) return link
    val isPost = host.lowercase(Locale.ROOT) in xHosts && "/status/" in path
    if (!isPost) return link
    return buildString {
        append("https://$provider$path")
        uri.rawQuery?.let { append('?').append(it) }
        uri.rawFragment?.let { append('#').append(it) }
    }
}
