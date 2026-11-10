// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.playback

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.YouTubeMusicPatchStrings
import app.reseam.youtubecommon.settings.YouTubeCommonSettings
import app.reseam.patches.youtubecommon.preferOriginalAudio
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.musicActivityOnCreate
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val forceOriginalAudio = patch(YouTubeMusicPatchStrings.forceOriginalAudio) {
    description(YouTubeMusicPatchStrings.forceOriginalAudioDescription)
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Playback, YouTubeMusicStrings.sectionAudio, YouTubeCommonSettings.forceOriginalAudio))

    execute { preferOriginalAudio(musicActivityOnCreate) }
}
