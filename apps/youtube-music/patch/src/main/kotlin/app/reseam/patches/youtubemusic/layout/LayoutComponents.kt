// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubecommon.hideById
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtubecommon.registerLithoFilter
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

val hideLayoutComponents = patch("Hide layout components") {
    description("Adds options to hide the Speed dial shelf and parts of the player.")
    compatibleWith(YOUTUBE_MUSIC)
    dependsOn(lithoFilter)
    settings(
        youTubeMusicSettings,
        section(YouTubeMusicSettingsPages.Interface, "Feed", YouTubeMusicSettings.hideSpeedDial),
        section(YouTubeMusicSettingsPages.Player, "Player",
            YouTubeMusicSettings.hideSongVideoSwitch, YouTubeMusicSettings.hideShuffleButton, YouTubeMusicSettings.hideRepeatButton),
    )

    execute {
        registerLithoFilter(LayoutComponentsFilter)
        hideById("audio_video_switch_pill_container", YouTubeMusicSettings.hideSongVideoSwitch)
        hideById("playback_queue_shuffle_button_view", YouTubeMusicSettings.hideShuffleButton)
        hideById("playback_queue_loop_button_view", YouTubeMusicSettings.hideRepeatButton)
    }
}

private object LayoutComponentsFilter : ExtClass("app.reseam.youtubemusic.layout.LayoutComponentsFilter")
