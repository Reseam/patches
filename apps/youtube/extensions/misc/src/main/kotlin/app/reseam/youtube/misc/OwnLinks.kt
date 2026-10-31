// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("OwnLinks")

package app.reseam.youtube.misc

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.reseam.runtime.Export

private val YOUTUBE_HOSTS = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be")

/**
 * YouTube opens its own links by sending them to itself and relies on owning youtube.com links.
 * Under another package name that would hand them to a different YouTube install.
 */
@Export
fun keepInApp(context: Context, intent: Intent, uri: Uri) {
    if (uri.host !in YOUTUBE_HOSTS) return
    val own = Intent(intent).setPackage(context.packageName)
    // The manifest accepts most YouTube paths, not all; the rest keep their original handler.
    if (own.resolveActivity(context.packageManager) != null) intent.setPackage(context.packageName)
}
