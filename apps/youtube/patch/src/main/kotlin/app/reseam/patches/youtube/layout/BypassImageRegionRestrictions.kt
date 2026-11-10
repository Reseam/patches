// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.imageRequestConstructor
import app.reseam.youtube.misc.bindings.ImageUrl
import app.reseam.youtube.settings.AppearanceSettings
import app.reseam.youtube.settings.YouTubeStrings

val bypassImageRegionRestrictions = patch(YouTubePatchStrings.bypassImageRegionRestrictions) {
    description(YouTubePatchStrings.bypassImageRegionRestrictionsDescription)
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Appearance, YouTubeStrings.sectionImages, AppearanceSettings.bypassImageRegionRestrictions))

    execute {
        // The single-argument constructor delegates here, so one hook covers every image load.
        imageRequestConstructor.before {
            param(0).assign(ImageUrl.override(param(0)))
        }
    }
}
