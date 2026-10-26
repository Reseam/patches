// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.core

import app.reseam.patch.appEntry
import app.reseam.patch.settings.section
import app.reseam.patch.settings.settingsHost
import app.reseam.gmscore.settings.GmsCoreStrings
import app.reseam.patches.bundle.reseamAbout
import app.reseam.patches.bundle.reseamBrand
import app.reseam.youtube.core.bindings.YouTubeContext
import app.reseam.youtubecommon.settings.YouTubeCommonSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val youTubeMusicSettings = settingsHost("youtube-music") {
    compatibleWith(YOUTUBE_MUSIC)
    brand(reseamBrand)
    about(reseamAbout)
    strings(GmsCoreStrings)

    settings(section(YouTubeMusicSettingsPages.Advanced, YouTubeMusicStrings.sectionDiagnostics, YouTubeCommonSettings.debugLogging))

    install {
        appEntry { YouTubeContext.init(application) }
    }
}
