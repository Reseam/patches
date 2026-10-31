// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

@file:JvmName("WatchHistoryDns")

package app.reseam.youtube.misc

import android.app.Activity
import android.app.AlertDialog
import app.reseam.bundle.Logger
import app.reseam.runtime.Export
import app.reseam.youtube.core.debug
import app.reseam.youtube.settings.YouTubeStrings
import java.net.InetAddress
import kotlin.concurrent.thread

private const val HISTORY_ENDPOINT = "s.youtube.com"
private const val CONTROL_DOMAIN = "youtube.com"

@Export
fun check(activity: Activity) {
    thread(name = "Reseam watch history DNS") {
        // Checking twice, each against a control domain, avoids warnings on a flaky connection.
        val blocked = (1..2).all { resolves(CONTROL_DOMAIN) && !resolves(HISTORY_ENDPOINT) }
        if (blocked) activity.runOnUiThread { warn(activity) }
    }
}

private fun warn(activity: Activity) {
    if (activity.isFinishing || activity.isDestroyed) return
    AlertDialog.Builder(activity)
        .setTitle(YouTubeStrings.watchHistoryDnsBlockedTitle)
        .setMessage(YouTubeStrings.watchHistoryDnsBlockedMessage(HISTORY_ENDPOINT))
        .setPositiveButton(android.R.string.ok, null)
        .show()
}

private fun resolves(host: String): Boolean =
    try {
        val address = InetAddress.getByName(host)
        (!address.isLoopbackAddress && !address.isAnyLocalAddress).also { valid ->
            if (!valid) Logger.debug { "$host resolves to ${address.hostAddress}" }
        }
    } catch (_: Exception) {
        Logger.debug { "$host does not resolve" }
        false
    }
