// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.playback

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubecommon.YouTubeCommonSettings
import app.reseam.patches.youtubecommon.preferOriginalAudio
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.musicActivityOnCreate
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

val forceOriginalAudio = patch("Force original audio") {
    description("Prefers the original audio track when a dubbed one is offered.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Playback, "Audio", YouTubeCommonSettings.forceOriginalAudio))

    execute { preferOriginalAudio(musicActivityOnCreate) }
}
