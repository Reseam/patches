// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.misc

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC_PACKAGE
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.patches.youtubespoof.SpoofSettings
import app.reseam.patches.youtubespoof.spoofUserAgentPackageName

val userAgentClientSpoof = patch("User-agent client spoof") {
    description("Keeps client user-agent package names compatible with YouTube Music's original package.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(
        youTubeMusicSettings,
        section(YouTubeMusicSettingsPages.Advanced, "Stream compatibility", SpoofSettings.userAgentClientSpoof),
    )

    execute { spoofUserAgentPackageName(YOUTUBE_MUSIC_PACKAGE) }
}
