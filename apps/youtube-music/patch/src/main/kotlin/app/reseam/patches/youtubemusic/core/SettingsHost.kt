// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.core

import app.reseam.patch.appEntry
import app.reseam.patch.settings.section
import app.reseam.patch.settings.settingsHost
import app.reseam.patches.youtubecommon.YouTubeCommonSettings
import app.reseam.patches.youtubecommon.YouTubeContext

val youTubeMusicSettings = settingsHost("youtube-music") {
    compatibleWith(YOUTUBE_MUSIC)

    settings(section(YouTubeMusicSettingsPages.Advanced, "Diagnostics", YouTubeCommonSettings.debugLogging))

    install {
        appEntry { call(YouTubeContext.init, application) }
    }
}
