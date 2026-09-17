// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.video

import app.reseam.patch.patch
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.video.quality.advancedVideoQualityMenu
import app.reseam.patches.youtube.video.quality.hidePremiumVideoQuality
import app.reseam.patches.youtube.video.quality.rememberVideoQuality
import app.reseam.patches.youtube.video.quality.videoQualityDialogButton

/** Umbrella for the quality controls and the two quality-menu implementations. */
val videoQuality = patch("Video quality") {
    description("Adds quality defaults, the advanced quality menu, and a quality player button.")
    compatibleWith(YOUTUBE)
    dependsOn(
        youTubeSettings,
        rememberVideoQuality,
        advancedVideoQualityMenu,
        hidePremiumVideoQuality,
        videoQualityDialogButton,
    )
}
