// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.misc

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.YouTubeMusicPatchStrings
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.musicActivityOnCreate
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubespoof.settings.SpoofSettings
import app.reseam.patches.youtubespoof.spoofStreams
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val spoofVideoStreams = patch(YouTubeMusicPatchStrings.spoofVideoStreams) {
    description(YouTubeMusicPatchStrings.spoofVideoStreamsDescription)
    compatibleWith(YOUTUBE_MUSIC)
    settings(
        youTubeMusicSettings,
        section(
            YouTubeMusicSettingsPages.Advanced,
            YouTubeMusicStrings.sectionStreamCompatibility,
            SpoofSettings.spoofVideoStreams,
            SpoofSettings.spoofVideoStreamsClient,
        ),
    )

    execute { spoofStreams(musicActivityOnCreate) }
}
