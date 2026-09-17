// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

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
import app.reseam.patches.youtube.internal.videoPlaybackStateHook

object CopyVideoUrl : ExtClass("app.reseam.youtube.buttons.CopyVideoUrl") {
    val initialize = static("initialize", Type.View)
}

val copyVideoUrl = patch("Copy video URL") {
    description("Adds player buttons that copy the current video's URL, optionally with its timestamp.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoPlaybackStateHook, playerControls)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Controls, "Player buttons", YouTubeSettings.copyVideoUrl, YouTubeSettings.copyVideoUrlTimestamp),
    )
    execute {
        resources.addId("reseam_copy_video_url_button")
        resources.addId("reseam_copy_video_url_timestamp_button")
        resources.addFile("drawable", "reseam_copy_video_url", "res/drawable/reseam_copy_video_url.xml",
            CopyVideoUrl::class.java.getResourceAsStream("/buttons/reseam_copy_video_url.xml")!!.readBytes())
        resources.addFile("drawable", "reseam_copy_video_url_timestamp", "res/drawable/reseam_copy_video_url_timestamp.xml",
            CopyVideoUrl::class.java.getResourceAsStream("/buttons/reseam_copy_video_url_timestamp.xml")!!.readBytes())
        registerPlayerControlLayout("/buttons/copy_video_url_timestamp.xml", "reseam_copy_video_url_timestamp_button", position = 0)
        registerPlayerControlLayout("/buttons/copy_video_url.xml", "reseam_copy_video_url_button", position = 1)
        registerPlayerControlInitializer(CopyVideoUrl.initialize)
    }
}
