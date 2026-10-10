// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.misc

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.musicActivityOnCreate
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.patches.youtubespoof.SpoofSettings
import app.reseam.patches.youtubespoof.spoofStreams

val spoofVideoStreams = patch("Spoof video streams") {
    description("Requests and installs playback streams from a compatible YouTube client.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(
        youTubeMusicSettings,
        section(
            YouTubeMusicSettingsPages.Advanced,
            "Stream compatibility",
            SpoofSettings.spoofVideoStreams,
            SpoofSettings.spoofVideoStreamsClient,
            SpoofSettings.spoofVideoStreamsTokenSource,
        ),
    )

    execute { spoofStreams(musicActivityOnCreate) }
}
