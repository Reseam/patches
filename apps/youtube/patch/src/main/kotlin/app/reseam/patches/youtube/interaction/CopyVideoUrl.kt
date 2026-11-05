// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.playerControls
import app.reseam.patches.youtube.internal.registerPlayerControlIcon
import app.reseam.patches.youtube.internal.registerPlayerControlInitializer
import app.reseam.patches.youtube.internal.registerPlayerControlLayout
import app.reseam.patches.youtube.internal.videoPlaybackStateHook
import app.reseam.youtube.buttons.bindings.CopyVideoUrl
import app.reseam.youtube.settings.PlayerControlsSettings
import app.reseam.youtube.settings.YouTubeStrings

val copyVideoUrl = patch("Copy video URL") {
    description("Adds player buttons that copy the current video's URL, optionally with its timestamp.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoPlaybackStateHook, playerControls)
    settings(
        youTubeSettings,
        section(YouTubeSettingsPages.Controls, YouTubeStrings.sectionPlayerButtons, PlayerControlsSettings.copyVideoUrl, PlayerControlsSettings.copyVideoUrlTimestamp),
    )
    execute {
        resources.addId("reseam_copy_video_url_button")
        resources.addId("reseam_copy_video_url_timestamp_button")
        registerPlayerControlIcon("reseam_copy_video_url", "/buttons/reseam_copy_video_url.xml",
            "/buttons/bold/reseam_copy_video_url.xml")
        registerPlayerControlIcon("reseam_copy_video_url_timestamp", "/buttons/reseam_copy_video_url_timestamp.xml",
            "/buttons/bold/reseam_copy_video_url_timestamp.xml")
        registerPlayerControlLayout("/buttons/copy_video_url_timestamp.xml", "/buttons/bold/copy_video_url_timestamp.xml",
            "reseam_copy_video_url_timestamp_button", position = 0)
        registerPlayerControlLayout("/buttons/copy_video_url.xml", "/buttons/bold/copy_video_url.xml",
            "reseam_copy_video_url_button", position = 1)
        registerPlayerControlInitializer(CopyVideoUrl.initialize)
    }
}
