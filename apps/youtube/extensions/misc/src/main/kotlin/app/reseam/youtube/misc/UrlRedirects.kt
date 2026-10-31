// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("UrlRedirects")

package app.reseam.youtube.misc

import android.net.Uri
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug

@Export
fun bypass(uri: Uri?): Uri? {
    if (uri == null || uri.path != "/redirect") return uri
    val host = uri.host ?: return uri
    if (host != "youtube.com" && !host.endsWith(".youtube.com")) return uri
    val target = uri.getQueryParameter("q") ?: return uri
    return Uri.parse(target).also { Logger.debug { "Bypassing redirect $uri to $it" } }
}
