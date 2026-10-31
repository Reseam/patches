// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout

import app.reseam.runtime.Export
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import app.reseam.youtube.litho.FilterRule
import app.reseam.youtube.player.PlayerType
import app.reseam.youtube.settings.CommentsSettings

@Export
class CommentsFilter : Filter() {
    private val aiSummaryIcon = ByteArrayFilterGroup(null, "yt_fill_spark_")

    init {
        addPathCallbacks(
            FilterRule(
                CommentsSettings.hideCommentsChannelGuidelines,
                "channel_guidelines_entry_banner",
            ),
            FilterRule(CommentsSettings.hideCommentsAiChatSummary, "live_chat_summary_banner.e"),
            FilterRule(CommentsSettings.hideCommentsAiSummary, "chip_bar.e") {
                PlayerType.current.isMaximizedOrFullscreen &&
                    aiSummaryIcon.check(it.buffer).isFiltered
            },
            FilterRule(
                CommentsSettings.hideCommentsByMembersHeader,
                "sponsorships_comments_header.e",
                "sponsorships_comments_footer.e",
            ),
            FilterRule(null, "video_metadata_carousel", "_comments") {
                if (it.path.startsWith("video_lockup_with_attachment.e")) {
                    CommentsSettings.hideCommentsSectionInHomeFeed.value
                } else {
                    CommentsSettings.hideCommentsSection.value
                }
            },
            FilterRule(CommentsSettings.hideCommentsCommunityGuidelines, "community_guidelines"),
            FilterRule(
                CommentsSettings.hideCommentsCreateAShortButton,
                "composer_short_creation_button.e",
            ),
            FilterRule(
                CommentsSettings.hideCommentsEmojiAndTimestampButtons,
                "|CellType|ContainerType|ContainerType|ContainerType|ContainerType|ContainerType|",
            ) {
                it.path.startsWith("comment_composer.e")
            },
            FilterRule(
                CommentsSettings.hideCommentsPreviewComment,
                "|carousel_item",
                "comments_entry_point_teaser",
                "comments_entry_point_simplebox",
            ),
            FilterRule(CommentsSettings.hideCommentsThanksButton, "super_thanks_button.e"),
        )
    }
}
