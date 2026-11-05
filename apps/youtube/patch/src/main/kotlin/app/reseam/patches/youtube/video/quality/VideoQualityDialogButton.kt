// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.video.quality

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.playerControls
import app.reseam.patches.youtube.internal.registerPlayerControlIcon
import app.reseam.patches.youtube.internal.registerPlayerControlInitializer
import app.reseam.patches.youtube.internal.registerPlayerControlLayout
import app.reseam.youtube.quality.bindings.VideoQualityDialogButton
import app.reseam.youtube.settings.PlayerControlsSettings
import app.reseam.youtube.settings.YouTubeStrings

val videoQualityDialogButton = patch("Video quality player button") {
    description("Adds a button that opens the current video's quality picker.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, playerControls)
    settings(youTubeSettings, section(YouTubeSettingsPages.Controls, YouTubeStrings.sectionPlayerButtons, PlayerControlsSettings.videoQualityDialogButton))

    execute {
        resources.addId("reseam_video_quality_button")
        registerPlayerControlIcon("reseam_video_quality", "/buttons/reseam_video_quality.xml", "/buttons/bold/reseam_text_box.xml")
        registerPlayerControlLayout("/buttons/video_quality.xml", "/buttons/bold/video_quality.xml",
            "reseam_video_quality_button_container", position = 5)
        registerPlayerControlInitializer(VideoQualityDialogButton.initialize)
    }
}
