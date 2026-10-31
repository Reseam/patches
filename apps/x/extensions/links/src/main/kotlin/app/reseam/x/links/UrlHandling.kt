// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("UrlHandling")

package app.reseam.x.links

import android.content.Intent
import app.reseam.runtime.Export
import app.reseam.x.settings.XSettings

val mirrorHosts = setOf("vxtwitter.com", "fixvx.com", "fxtwitter.com", "fixupx.com", "twittpr.com", "fixtweet.com", "girlcockx.com")

@Export
fun normalizeIntent(intent: Intent?) {
    if (!XSettings.customUrlHandling.value) return
    val uri = intent?.data ?: return
    val host = uri.host?.lowercase()?.removePrefix("www.") ?: return
    if (host in mirrorHosts) intent.data = uri.buildUpon().authority("x.com").build()
}
