// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui

import app.reseam.runtime.Export
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup
import app.reseam.youtube.litho.FilterGroupList.StringFilterGroupList
import app.reseam.youtube.litho.FilterRule
import app.reseam.youtube.settings.PlayerOverlaySettings

@Export
class VideoActionButtonsFilter : Filter() {
    private val accessibilityGroups =
        StringFilterGroupList().apply {
            addAll(
                StringFilterGroup(PlayerOverlaySettings.hideShareButton, "id.video.share.button"),
                StringFilterGroup(PlayerOverlaySettings.hideRemixButton, "id.video.remix.button"),
            )
        }

    init {
        addIdentifierCallbacks(
            FilterRule(null, "video_action_bar.e") {
                pathCallbacks.all { it.isEnabled } && accessibilityGroups.all { it.isEnabled }
            }
        )
        // YouTube serves both separate like and dislike controls and a combined segmented one.
        addPathCallbacks(
            FilterRule(PlayerOverlaySettings.disableLikeSubscribeGlow, "animated_button_border.e") {
                isActionBar(it.path) || it.path.startsWith("compact_channel_bar.e")
            },
            FilterRule(
                PlayerOverlaySettings.hideLikeDislikeButton,
                "|like_button_with_vm_input.e",
                "|dislike_button_vm.e",
                "|segmented_like_dislike_button.e",
            ),
            FilterRule(PlayerOverlaySettings.hideDownloadButton, "|download_button.e"),
            FilterRule(PlayerOverlaySettings.hideSaveButton, "|save_to_playlist_button"),
            FilterRule(null, "|ContainerType|button.e") {
                isActionBar(it.path) && accessibilityGroups.check(it.accessibility).isFiltered
            },
        )
    }
}

private fun isActionBar(path: String): Boolean = actionBarPrefixes.any(path::startsWith)

private val actionBarPrefixes = listOf("video_action_bar.e", "compactify_video_action_bar.e")
