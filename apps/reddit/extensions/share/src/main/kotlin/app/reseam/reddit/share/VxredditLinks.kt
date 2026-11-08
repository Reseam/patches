// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("VxredditLinks")

package app.reseam.reddit.share

import app.reseam.runtime.Export
import java.net.URI
import java.net.URISyntaxException

/**
 * A post link on vxreddit.com, or null for anything that is not a reddit.com post. The query only
 * carries Reddit's share tracking, so it is dropped. The link is null when the suspended shortener
 * resumes.
 */
@Export
fun rewrite(link: String?): String? {
    val uri =
        try {
            URI(link?.trim() ?: return null)
        } catch (_: URISyntaxException) {
            return null
        }
    val host = uri.host ?: return null
    val path = uri.rawPath ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true)) return null
    if (host != "reddit.com" && !host.endsWith(".reddit.com")) return null
    if ("/comments/" !in path) return null
    return "https://vxreddit.com$path"
}
