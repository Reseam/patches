// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.PlayerOverlaySettings
import app.reseam.youtube.settings.YouTubeStrings

val hideAutoplayPreview = patch(YouTubePatchStrings.hideAutoplayPreview) {
    description(YouTubePatchStrings.hideAutoplayPreviewDescription)
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, YouTubeStrings.sectionPlayerOverlay, PlayerOverlaySettings.hideAutoplayPreview))

    execute {
        val previewStub = resources.id("id", "autonav_preview_stub")?.toLong()
            ?: error("id/autonav_preview_stub is missing")
        playerLayoutInitializer.skipInflationWhen(
            previewStub,
            PlayerOverlaySettings.hideAutoplayPreview,
        )
    }
}
