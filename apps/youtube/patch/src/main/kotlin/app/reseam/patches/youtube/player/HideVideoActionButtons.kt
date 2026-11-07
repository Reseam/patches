// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtubecommon.registerLithoFilter
import app.reseam.youtube.playerui.bindings.VideoActionButtonsFilter
import app.reseam.youtube.settings.PlayerOverlaySettings
import app.reseam.youtube.settings.YouTubeStrings

val hideVideoActionButtons = patch("Hide video action buttons") {
    description("Hides selected action buttons below videos.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, lithoFilter)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Overlay,
            YouTubeStrings.sectionVideoActionButtons,
            PlayerOverlaySettings.disableLikeSubscribeGlow,
            PlayerOverlaySettings.hideLikeDislikeButton,
            PlayerOverlaySettings.hideDownloadButton,
            PlayerOverlaySettings.hideRemixButton,
            PlayerOverlaySettings.hideSaveButton,
            PlayerOverlaySettings.hideShareButton,
            PlayerOverlaySettings.hideAskButton,
        ),
    )

    execute { registerLithoFilter(VideoActionButtonsFilter) }
}
