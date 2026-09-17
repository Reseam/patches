// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.after
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

private const val URI = "android.net.Uri"

val bypassUrlRedirects = patch("Bypass URL redirects") {
    description("Opens a link directly instead of through youtube.com/redirect, which logs the click.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, "Redirects", YouTubeSettings.bypassUrlRedirects))

    execute {
        val sites = parsedUris.all
        check(sites.isNotEmpty()) { "No URI conversion lambdas" }
        sites.forEach { site ->
            site.previous { resultOf(URI) }.captureAs("uri", URI)
                .after(YouTubeSettings.bypassUrlRedirects) {
                    capture("uri").assign(call(UrlRedirects.bypass, capture("uri")))
                }
        }
    }
}

private val parsedUris = methods("URI conversion lambdas") {
    params(Type.Object)
    returns(Type.Object)
    calls { owner("Landroid/net/Uri;"); name("parse") }
}.points("returned URI") {
    invokeStatic { owner(URI); name("parse"); params(Type.String) }
    then { resultOf(URI) }
    then { opcode(Opcode.RETURN_OBJECT) }
}

object UrlRedirects : ExtClass("app.reseam.youtube.misc.UrlRedirects") {
    val bypass = static("bypass", URI, returns = URI)
}
