// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patches.youtubecommon.Launcher
import app.reseam.patches.youtubecommon.customBrandingFor
import app.reseam.patches.youtubemusic.core.MUSIC_ACTIVITY
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.musicActivityOnCreate
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

val customBranding = customBrandingFor(
    YOUTUBE_MUSIC,
    youTubeMusicSettings,
    YouTubeMusicSettingsPages.Interface,
    "Branding",
    YouTubeMusicSettings.customBrandingName,
    YouTubeMusicSettings.customBrandingIcon,
    Launcher(
        activity = MUSIC_ACTIVITY,
        entry = MUSIC_ACTIVITY,
        originalIcon = "@mipmap/ic_launcher_release",
        // The activity also handles every music.youtube.com link; only the launcher filter moves.
        movesFilter = { filter -> filter.children.any { it.tag == "action" && it["android:name"] == "android.intent.action.MAIN" } },
    ),
    musicActivityOnCreate,
)
