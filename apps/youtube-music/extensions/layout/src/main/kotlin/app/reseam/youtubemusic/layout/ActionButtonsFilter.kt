// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtubemusic.layout

import app.reseam.runtime.Export
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import app.reseam.youtube.litho.FilterGroupList.ByteArrayFilterGroupList
import app.reseam.youtube.litho.FilterRule
import app.reseam.youtubemusic.settings.YouTubeMusicSettings

@Export
class ActionButtonsFilter : Filter() {
    // Match icon names without the theme prefix, which varies between server experiments.
    private val icons =
        ByteArrayFilterGroupList().apply {
            addAll(
                ByteArrayFilterGroup(YouTubeMusicSettings.hideCommentsButton, "_text_bubble_"),
                ByteArrayFilterGroup(YouTubeMusicSettings.hideSaveButton, "_playlist_add_"),
                ByteArrayFilterGroup(YouTubeMusicSettings.hideShareButton, "_share_"),
                ByteArrayFilterGroup(YouTubeMusicSettings.hideLyricsButton, "_quote_"),
                ByteArrayFilterGroup(YouTubeMusicSettings.hideMixButton, "_mix_"),
            )
        }

    init {
        addIdentifierCallbacks(
            FilterRule(null, ACTION_BAR) {
                pathCallbacks.all { it.isEnabled } && icons.all { it.isEnabled }
            }
        )
        addPathCallbacks(
            FilterRule(
                YouTubeMusicSettings.hideLikeDislikeButton,
                "|segmented_like_dislike_button.e",
            ) {
                it.path.startsWith(ACTION_BAR)
            },
            FilterRule(YouTubeMusicSettings.hideDownloadButton, "|music_download_button.e") {
                it.path.startsWith(ACTION_BAR)
            },
            FilterRule(null, "|button_inner.e") {
                it.path.startsWith(ACTION_BAR) && icons.check(it.buffer).isFiltered
            },
        )
    }
}

private const val ACTION_BAR = "video_action_bar.e"
