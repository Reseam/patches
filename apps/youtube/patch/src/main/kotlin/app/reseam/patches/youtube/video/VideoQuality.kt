// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.video

import app.reseam.patch.patch
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.video.quality.advancedVideoQualityMenu
import app.reseam.patches.youtube.video.quality.hidePremiumVideoQuality
import app.reseam.patches.youtube.video.quality.rememberVideoQuality
import app.reseam.patches.youtube.video.quality.videoQualityDialogButton

/** Umbrella for the quality controls and the two quality-menu implementations. */
val videoQuality = patch(YouTubePatchStrings.videoQuality) {
    description(YouTubePatchStrings.videoQualityDescription)
    compatibleWith(YOUTUBE)
    dependsOn(
        youTubeSettings,
        rememberVideoQuality,
        advancedVideoQualityMenu,
        hidePremiumVideoQuality,
        videoQualityDialogButton,
    )
}
