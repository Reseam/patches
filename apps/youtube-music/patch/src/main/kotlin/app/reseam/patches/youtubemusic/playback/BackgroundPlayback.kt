// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.playback

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtubecommon.playableInBackground
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.youtubemusic.settings.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val backgroundPlayback = patch("Background playback") {
    description("Keeps playing when the app is in the background or the screen is off.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Playback, YouTubeMusicStrings.sectionBackground, YouTubeMusicSettings.backgroundPlayback))

    execute {
        gate(YouTubeMusicSettings.backgroundPlayback) {
            playableInBackground.alwaysReturn(true)
        }
    }
}
