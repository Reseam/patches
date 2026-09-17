// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.layout

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.openVideosFullscreenHook

val openVideosFullscreen = patch("Open videos fullscreen") {
    description("Opens videos in fullscreen portrait mode when YouTube starts them.")
    compatibleWith(YOUTUBE)
    dependsOn(openVideosFullscreenHook)
    settings(youTubeSettings, section(YouTubeSettingsPages.Player, "Player", YouTubeSettings.openVideosFullscreen))
}
