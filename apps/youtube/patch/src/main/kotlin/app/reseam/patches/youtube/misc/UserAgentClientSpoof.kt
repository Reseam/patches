// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YOUTUBE_PACKAGE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtubespoof.settings.SpoofSettings
import app.reseam.patches.youtubespoof.spoofUserAgentPackageName
import app.reseam.youtube.settings.YouTubeStrings

val userAgentClientSpoof = patch("User-agent client spoof") {
    description("Keeps client user-agent package names compatible with YouTube's original package.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, YouTubeStrings.sectionStreamCompatibility, SpoofSettings.userAgentClientSpoof))

    execute { spoofUserAgentPackageName(YOUTUBE_PACKAGE) }
}
