// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ShareLinks")

package app.reseam.x.share

import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

private val xHosts =
    setOf("x.com", "www.x.com", "twitter.com", "www.twitter.com", "mobile.twitter.com")

@Export
fun rewrite(link: String?): String? {
    if (link == null) return null
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
    if (!isHttp || host.lowercase(Locale.ROOT) !in xHosts) return link
    val provider = XSettings.shareLinkProvider.value.takeIf { it != "off" && "/status/" in path }
    // Shared trends are searches, whose query is the search itself.
    val clean = XSettings.removeShareTracking.value && path != "/search"
    return buildString {
        append(if (provider != null) "https://$provider" else "${uri.scheme}://${uri.rawAuthority}")
        append(path)
        if (!clean) {
            uri.rawQuery?.let { append('?').append(it) }
            uri.rawFragment?.let { append('#').append(it) }
        }
    }
}
