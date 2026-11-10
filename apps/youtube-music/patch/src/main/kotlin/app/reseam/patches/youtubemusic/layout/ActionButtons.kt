// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubemusic.layout

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtubecommon.registerLithoFilter
import app.reseam.patches.youtubemusic.YouTubeMusicPatchStrings
import app.reseam.patches.youtubemusic.core.YOUTUBE_MUSIC
import app.reseam.patches.youtubemusic.core.YouTubeMusicSettingsPages
import app.reseam.patches.youtubemusic.core.youTubeMusicSettings
import app.reseam.youtubemusic.layout.bindings.ActionButtonsFilter
import app.reseam.youtubemusic.settings.YouTubeMusicSettings
import app.reseam.youtubemusic.settings.YouTubeMusicStrings

val hideActionButtons = patch(YouTubeMusicPatchStrings.hideActionButtons) {
    description(YouTubeMusicPatchStrings.hideActionButtonsDescription)
    compatibleWith(YOUTUBE_MUSIC)
    dependsOn(lithoFilter)
    settings(
        youTubeMusicSettings,
        with(YouTubeMusicSettings) {
            section(
                YouTubeMusicSettingsPages.Player, YouTubeMusicStrings.sectionActionButtons,
                hideLikeDislikeButton, hideDownloadButton, hideCommentsButton, hideSaveButton,
                hideShareButton, hideLyricsButton, hideMixButton,
            )
        },
    )

    execute { registerLithoFilter(ActionButtonsFilter) }
}
