// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.video.quality

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.playerControls
import app.reseam.patches.youtube.internal.registerPlayerControlInitializer
import app.reseam.patches.youtube.internal.registerPlayerControlLayout

object VideoQualityDialogButton : ExtClass("app.reseam.youtube.quality.VideoQualityDialogButton") {
    val initialize = static("initialize", Type.View)
}

val videoQualityDialogButton = patch("Video quality player button") {
    description("Adds a button that opens the current video's quality picker.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, playerControls)
    settings(youTubeSettings, section(YouTubeSettingsPages.Controls, "Player buttons", YouTubeSettings.videoQualityDialogButton))

    execute {
        resources.addId("reseam_video_quality_button")
        resources.addFile(
            "drawable",
            "reseam_video_quality",
            "res/drawable/reseam_video_quality.xml",
            VideoQualityDialogButton::class.java.getResourceAsStream("/buttons/reseam_video_quality.xml")!!.readBytes(),
        )
        registerPlayerControlLayout("/buttons/video_quality.xml", "reseam_video_quality_button_container", position = 5)
        registerPlayerControlInitializer(VideoQualityDialogButton.initialize)
    }
}
