// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.links

import app.reseam.instagram.links.bindings.ExternalLinks
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.instagram.settings.LinkSettings
import app.reseam.patch.Type
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val openLinksExternally = patch("Open links in external browser") {
    description("Opens web links in your default browser instead of Instagram's in-app browser.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionLinks, LinkSettings.openExternally))

    execute {
        // The launcher reports where the link opened.
        val launchedInExternalBrowser = klass(browserLaunch.returnType).method("<clinit>")
            .point { string("EXTERNAL_BROWSER") }
            .next { opcode(Opcode.SPUT_OBJECT) }
            .field("launchedInExternalBrowser")
        gate(LinkSettings.openExternally) {
            browserLaunch.before {
                val launcher = paramOfType(browserLaunch.owner)
                whenTrue(ExternalLinks.open(launcher.fieldOfType(Type.Context), launcher.field(launcherUrl))) {
                    returnValue(staticField(launchedInExternalBrowser))
                }
            }
        }
    }
}

private val browserLaunch = method("browserLaunch") {
    strings("Tracking.ARG_CLICK_SOURCE", "BrowserLauncher")
}

private val launcherUrl = browserLaunch
    .point { string("Tracking.ARG_CLICK_SOURCE") }
    .next { field { type(Type.String) } }
    .field("launcherUrl")
