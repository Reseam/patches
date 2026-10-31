// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ExternalLinks")

package app.reseam.instagram.links

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.reseam.runtime.Export
import java.util.Locale

private const val LINK_SHIM = "l.instagram.com"
// Checkout, account login and Bloks forms need the in-app browser's JS bridge.
private val inAppDomains = listOf("instagram.com", "facebook.com", "meta.com")
private val webSchemes = listOf("http", "https")

@Export
fun open(context: Context, url: String?): Boolean {
    val uri = external(url) ?: return false
    return try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

private fun external(url: String?): Uri? {
    if (url == null) return null
    val uri = Uri.parse(url)
    if (webSchemes.none { it.equals(uri.scheme, ignoreCase = true) }) return null
    val host = uri.host?.lowercase(Locale.getDefault()) ?: return null
    if (host == LINK_SHIM) return external(uri.getQueryParameter("u"))
    return uri.takeUnless {
        inAppDomains.any { domain -> host == domain || host.endsWith(".$domain") }
    }
}
