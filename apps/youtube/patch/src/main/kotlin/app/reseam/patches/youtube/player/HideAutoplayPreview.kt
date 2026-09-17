// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.Type
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val hideAutoplayPreview = patch("Hide autoplay preview") {
    description("Stops the autoplay preview from appearing over the player.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, "Player overlay", YouTubeSettings.hideAutoplayPreview))

    execute {
        val previewStub = resources.id("id", "autonav_preview_stub")?.toLong()
            ?: error("id/autonav_preview_stub is missing")
        playerLayoutInitializer.skipInflationWhen(
            previewStub,
            YouTubeSettings.hideAutoplayPreview,
        )
    }
}
