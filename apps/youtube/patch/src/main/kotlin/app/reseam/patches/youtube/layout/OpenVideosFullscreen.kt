// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.openVideosFullscreenHook
import app.reseam.youtube.settings.PlayerSettings
import app.reseam.youtube.settings.YouTubeStrings

val openVideosFullscreen = patch(YouTubePatchStrings.openVideosFullscreen) {
    description(YouTubePatchStrings.openVideosFullscreenDescription)
    compatibleWith(YOUTUBE)
    dependsOn(openVideosFullscreenHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Player, YouTubeStrings.sectionPlayer, PlayerSettings.openVideosFullscreen))
}
