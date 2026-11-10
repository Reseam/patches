// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.video.speed

import app.reseam.patch.patch
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.video.speed.customPlaybackSpeed
import app.reseam.patches.youtube.video.speed.playbackSpeedDialogButton
import app.reseam.patches.youtube.video.speed.rememberPlaybackSpeed

/** Umbrella for the modern and legacy playback-speed controls. */
val playbackSpeed = patch(YouTubePatchStrings.playbackSpeed) {
    description(YouTubePatchStrings.playbackSpeedDescription)
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, customPlaybackSpeed, rememberPlaybackSpeed, playbackSpeedDialogButton)
}
