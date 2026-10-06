// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.ExtClass
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtubecommon.registerLithoFilter
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettings
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings

val hideActionButtons = patch("Hide action buttons") {
    description("Adds options to hide the buttons below the player.")
    compatibleWith(YOUTUBE_MUSIC)
    dependsOn(lithoFilter)
    settings(
        youTubeMusicSettings,
        with(YouTubeMusicSettings) {
            section(
                YouTubeMusicSettingsPages.Player, "Action buttons",
                hideLikeDislikeButton, hideDownloadButton, hideCommentsButton, hideSaveButton,
                hideShareButton, hideLyricsButton, hideMixButton,
            )
        },
    )

    execute { registerLithoFilter(ActionButtonsFilter) }
}

private object ActionButtonsFilter : ExtClass("app.reseam.youtubemusic.layout.ActionButtonsFilter")
