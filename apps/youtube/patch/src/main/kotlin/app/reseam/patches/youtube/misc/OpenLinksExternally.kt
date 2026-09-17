// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.Type
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.after
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

private const val CUSTOM_TABS_SERVICE = "android.support.customtabs.action.CustomTabsService"

val openLinksExternally = patch("Open links externally") {
    description("Opens links in your browser instead of YouTube's in-app browser.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, "Links", YouTubeSettings.openLinksExternally))

    execute {
        // An empty action finds no Custom Tabs provider, so the link falls through to the browser.
        customTabsServiceLookups.points { string(CUSTOM_TABS_SERVICE) }.forEach {
            captureAs("customTabsAction", Type.String)
                .after(YouTubeSettings.openLinksExternally) { capture("customTabsAction").assign(string("")) }
        }
    }
}

val customTabsServiceLookups = methods("customTabsServiceLookups") { strings(CUSTOM_TABS_SERVICE) }
