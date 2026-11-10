// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.misc

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.YouTubeMusicPatchStrings
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC_PACKAGE
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubespoof.settings.SpoofSettings
import app.reseam.patches.youtubespoof.spoofUserAgentPackageName
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val userAgentClientSpoof = patch(YouTubeMusicPatchStrings.userAgentClientSpoof) {
    description(YouTubeMusicPatchStrings.userAgentClientSpoofDescription)
    compatibleWith(YOUTUBE_MUSIC)
    settings(
        youTubeMusicSettings,
        section(YouTubeMusicSettingsPages.Advanced, YouTubeMusicStrings.sectionStreamCompatibility, SpoofSettings.userAgentClientSpoof),
    )

    execute { spoofUserAgentPackageName(YOUTUBE_MUSIC_PACKAGE) }
}
