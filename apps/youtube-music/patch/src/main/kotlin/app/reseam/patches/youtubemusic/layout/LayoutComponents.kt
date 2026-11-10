// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubecommon.hideById
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtubecommon.registerLithoFilter
import app.reseam.patches.youtubemusic.YouTubeMusicPatchStrings
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubemusic.layout.bindings.LayoutComponentsFilter
import app.reseam.youtubemusic.settings.YouTubeMusicSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val hideLayoutComponents = patch(YouTubeMusicPatchStrings.hideLayoutComponents) {
    description(YouTubeMusicPatchStrings.hideLayoutComponentsDescription)
    compatibleWith(YOUTUBE_MUSIC)
    dependsOn(lithoFilter)
    settings(
        youTubeMusicSettings,
        section(YouTubeMusicSettingsPages.Interface, YouTubeMusicStrings.sectionFeed, YouTubeMusicSettings.hideSpeedDial),
        section(YouTubeMusicSettingsPages.Player, YouTubeMusicStrings.sectionPlayer,
            YouTubeMusicSettings.hideSongVideoSwitch, YouTubeMusicSettings.hideShuffleButton, YouTubeMusicSettings.hideRepeatButton),
    )

    execute {
        registerLithoFilter(LayoutComponentsFilter)
        hideById("audio_video_switch_pill_container", YouTubeMusicSettings.hideSongVideoSwitch)
        hideById("playback_queue_shuffle_button_view", YouTubeMusicSettings.hideShuffleButton)
        hideById("playback_queue_loop_button_view", YouTubeMusicSettings.hideRepeatButton)
    }
}
