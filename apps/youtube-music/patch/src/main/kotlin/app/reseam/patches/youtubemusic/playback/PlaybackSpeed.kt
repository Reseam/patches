// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.playback

import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubecommon.setPlaybackRate
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubemusic.layout.bindings.PlaybackSpeed
import app.reseam.youtubemusic.settings.YouTubeMusicSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val playbackSpeed = patch("Playback speed") {
    description("Plays every song at the speed you choose.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Playback, YouTubeMusicStrings.sectionSpeed, YouTubeMusicSettings.playbackSpeed))

    execute {
        setPlaybackRate.before { param(0).assign(PlaybackSpeed.rate(param(0))) }
    }
}
