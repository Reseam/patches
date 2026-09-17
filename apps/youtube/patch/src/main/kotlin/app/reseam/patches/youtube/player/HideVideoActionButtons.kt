// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.ExtClass
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.lithoFilter
import app.reseam.patches.youtube.internal.registerLithoFilter

val hideVideoActionButtons = patch("Hide video action buttons") {
    description("Hides selected action buttons below videos.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, lithoFilter)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Overlay,
            "Video action buttons",
            YouTubeSettings.disableLikeSubscribeGlow,
            YouTubeSettings.hideLikeDislikeButton,
            YouTubeSettings.hideDownloadButton,
            YouTubeSettings.hideRemixButton,
            YouTubeSettings.hideSaveButton,
            YouTubeSettings.hideShareButton,
        ),
    )

    execute { registerLithoFilter(VideoActionButtonsFilter) }
}

object VideoActionButtonsFilter : ExtClass("app.reseam.youtube.playerui.VideoActionButtonsFilter")
