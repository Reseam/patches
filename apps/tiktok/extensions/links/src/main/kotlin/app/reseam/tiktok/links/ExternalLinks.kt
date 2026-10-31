// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ExternalLinks")

package app.reseam.tiktok.links

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.reseam.runtime.Export
import java.util.Locale

private const val WEB_PAGE = "webview"
// TikTok's own pages need its in-app browser: login, Help Center, Shop and payments.
private val inAppDomains =
    listOf(
        "tiktok.com",
        "tiktokv.com",
        "tiktokv.us",
        "tiktokw.us",
        "tiktokshop.com",
        "bytedance.com",
        "byteoversea.com",
    )
private val webSchemes = listOf("http", "https")
// TikTok's warning page in front of outside links; the link itself is its target parameter.
private val linkSafety = Regex("""(^|\.)tiktoklinksafety\.[a-z]+$""")

/** Opens the web page a route shows in the browser instead; false leaves the route to TikTok. */
@Export
fun open(context: Context?, route: String?, data: Uri?): Boolean =
    launch(context, webPage(route?.let(Uri::parse) ?: data))

/**
 * Opens a third-party page, such as a bio link, in the browser instead; false leaves it to TikTok.
 */
@Export
fun openPage(context: Context?, url: String?): Boolean =
    launch(context, url?.let(Uri::parse)?.let(::outsidePage))

private fun launch(context: Context?, page: Uri?): Boolean {
    if (context == null || page == null) return false
    return try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, page).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private fun webPage(route: Uri?): Uri? {
    if (route?.host != WEB_PAGE) return null
    return route.getQueryParameter("url")?.let(Uri::parse)?.let(::outsidePage)
}

private fun outsidePage(page: Uri): Uri? {
    if (webSchemes.none { it.equals(page.scheme, ignoreCase = true) }) return null
    val host = page.host?.lowercase(Locale.ROOT) ?: return null
    if (linkSafety.containsMatchIn(host))
        return page.getQueryParameter("target")?.let(Uri::parse)?.let(::outsidePage)
    return page.takeUnless {
        inAppDomains.any { domain -> host == domain || host.endsWith(".$domain") }
    }
}
