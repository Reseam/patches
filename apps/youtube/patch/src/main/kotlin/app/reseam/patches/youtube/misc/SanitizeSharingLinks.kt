// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtubecommon.settings.YouTubeCommonSettings
import app.reseam.patches.youtubecommon.sanitizeSharedLinks
import app.reseam.youtube.settings.YouTubeStrings

val sanitizeSharingLinks = patch("Sanitize sharing links") {
    description("Strips the tracking parameters YouTube adds to a link before you share it.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, YouTubeStrings.sectionPrivacy, YouTubeCommonSettings.sanitizeSharingLinks))

    execute { sanitizeSharedLinks(YouTubeCommonSettings.sanitizeSharingLinks) }
}
