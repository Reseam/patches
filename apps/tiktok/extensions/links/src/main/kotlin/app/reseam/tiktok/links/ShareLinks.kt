// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("ShareLinks")

package app.reseam.tiktok.links

import android.net.Uri
import app.reseam.runtime.Export

/** The link without its query, which carries the sharer's tracking parameters. */
@Export
fun clean(link: String?): String? {
    if (link.isNullOrEmpty()) return link
    return Uri.parse(link).buildUpon().clearQuery().fragment(null).build().toString()
}
