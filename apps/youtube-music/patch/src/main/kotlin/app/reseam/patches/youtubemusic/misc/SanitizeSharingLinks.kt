// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.misc

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.youtubecommon.settings.YouTubeCommonSettings
import app.reseam.patches.youtubecommon.sanitizeSharedLinks
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val sanitizeSharingLinks = patch("Sanitize sharing links") {
    description("Strips the tracking parameters YouTube Music adds to a link before you share it.")
    compatibleWith(YOUTUBE_MUSIC)
    settings(youTubeMusicSettings, section(YouTubeMusicSettingsPages.Advanced, YouTubeMusicStrings.sectionPrivacy, YouTubeCommonSettings.sanitizeSharingLinks))

    execute { sanitizeSharedLinks(YouTubeCommonSettings.sanitizeSharingLinks) }
}
