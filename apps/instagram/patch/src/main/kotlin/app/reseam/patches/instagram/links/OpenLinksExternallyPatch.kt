// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.links

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.ExternalLinks
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.LinkSettings
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val openLinksExternally = patch("Open links in external browser") {
    description("Opens web links in your default browser instead of Instagram's in-app browser.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Links", LinkSettings.openExternally))

    execute {
        gate(LinkSettings.openExternally) {
            browserLaunch.before {
                val launcher = paramOfType(browserLaunch.owner)
                whenTrue(call(ExternalLinks.open, launcher.fieldOfType(Type.Context), launcher.field(launcherUrl))) { returnTrue() }
            }
        }
    }
}

private val browserLaunch = method("browserLaunch") {
    strings("TrackingInfo.ARG_MODULE_NAME", "BrowserLauncher")
    returns(Type.Boolean)
}

private val launcherUrl = browserLaunch
    .point { string("Tracking.ARG_CLICK_SOURCE") }
    .next { field { type(Type.String) } }
    .field("launcherUrl")
