// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.playback

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubecommon.setPlaybackRate
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

val playbackSpeed = patch("Playback speed") {
    description("Plays every song at the speed you choose.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Playback, "Speed", YouTubeMusicSettings.playbackSpeed))

    execute {
        setPlaybackRate.before { param(0).assign(call(PlaybackSpeed.rate, param(0))) }
    }
}

private object PlaybackSpeed : ExtClass("app.reseam.youtubemusic.layout.PlaybackSpeed") {
    val rate by static(Type.Float, returns = Type.Float)
}
