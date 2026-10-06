// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.playback

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtubecommon.playableInBackground
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

val backgroundPlayback = patch("Background playback") {
    description("Keeps playing when the app is in the background or the screen is off.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Playback, "Background", YouTubeMusicSettings.backgroundPlayback))

    execute {
        gate(YouTubeMusicSettings.backgroundPlayback) {
            playableInBackground.alwaysReturn(true)
        }
    }
}
