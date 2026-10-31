// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.links

import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.x.core.MAIN_ACTIVITY
import app.reseam.patches.x.core.X
import app.reseam.x.links.bindings.UrlHandling
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings
import app.reseam.patches.x.core.xSettings

// Keep in sync with UrlHandling.mirrorHosts in the extension.
private val mirrorHosts = listOf("vxtwitter.com", "fixvx.com", "fxtwitter.com", "fixupx.com", "twittpr.com", "fixtweet.com", "girlcockx.com")

val customUrlHandling = patch("Custom URL handling") {
    description("Opens fixvx.com, vxtwitter.com and similar mirror links in the app, like x.com links.")
    compatibleWith(X)
    settings(xSettings, section(XStrings.sectionLinks, XSettings.customUrlHandling))

    execute {
        intentHandler.before { UrlHandling.normalizeIntent(param(0)) }

        manifest.edit {
            val filters = findByAttribute("android:host", "x.com").mapNotNull { it.parent }.distinct()
            require(filters.isNotEmpty()) { "no x.com deep link filter on $MAIN_ACTIVITY" }
            for (filter in filters) {
                val existing = filter.children.filter { it.tag == "data" }.mapNotNull { it["android:host"] }.toSet()
                for (host in mirrorHosts - existing) {
                    filter.appendChild(createElement("data").apply { this["android:host"] = host })
                }
            }
        }
    }
}

// The incoming VIEW-intent handler, before it matches the link's path to a screen; param 0 is the intent.
private val intentHandler = method("intentHandler") {
    strings("DefaultIntentHandler", "No matching route found for URI: ")
}
