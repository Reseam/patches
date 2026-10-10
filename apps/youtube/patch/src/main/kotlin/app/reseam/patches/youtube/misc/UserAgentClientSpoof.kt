// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YOUTUBE_PACKAGE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtubespoof.SpoofSettings
import app.reseam.patches.youtubespoof.spoofUserAgentPackageName

val userAgentClientSpoof = patch("User-agent client spoof") {
    description("Keeps client user-agent package names compatible with YouTube's original package.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, "Stream compatibility", SpoofSettings.userAgentClientSpoof))

    execute { spoofUserAgentPackageName(YOUTUBE_PACKAGE) }
}
