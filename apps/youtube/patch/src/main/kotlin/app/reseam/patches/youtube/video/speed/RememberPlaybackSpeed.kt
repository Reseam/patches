// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.video.speed

import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.onCreateHook
import app.reseam.patches.youtube.internal.userSelectedPlaybackSpeedHook
import app.reseam.patches.youtube.internal.videoInformationHook
import app.reseam.youtube.settings.VideoSettings
import app.reseam.youtube.settings.YouTubeStrings
import app.reseam.youtube.speed.bindings.RememberPlaybackSpeed

val rememberPlaybackSpeed = patch(YouTubePatchStrings.rememberPlaybackSpeed) {
    description(YouTubePatchStrings.rememberPlaybackSpeedDescription)
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, videoInformationHook, customPlaybackSpeed)
    settings(youTubeSettings, section(YouTubeSettingsPages.Video, YouTubeStrings.sectionPlaybackSpeed, VideoSettings.playbackSpeedDefault, VideoSettings.rememberPlaybackSpeed))

    execute {
        onCreateHook(RememberPlaybackSpeed.newVideoStarted)
        userSelectedPlaybackSpeedHook(RememberPlaybackSpeed.userSelectedPlaybackSpeed)
        oldPlaybackSpeedMenu.before { RememberPlaybackSpeed.applyDefaultPlaybackSpeed() }
    }
}
