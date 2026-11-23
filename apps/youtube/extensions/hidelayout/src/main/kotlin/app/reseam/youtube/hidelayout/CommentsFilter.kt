// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.hidelayout

import app.reseam.runtime.Export
import app.reseam.youtube.core.Proto
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import app.reseam.youtube.litho.FilterRule
import app.reseam.youtube.player.PlayerType
import app.reseam.youtube.settings.CommentsSettings

@Export
class CommentsFilter : Filter() {
    private val aiSummaryIcon = ByteArrayFilterGroup(null, "yt_fill_spark_")
    private val quickEmoji = ByteArrayFilterGroup(null, "id.comment.quick_emoji.button")
    private val composerControls = ByteArrayFilterGroup(null, "composer_input", "composer_timestamp_button")

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
                "composer_timestamp_button.e",
            ),
            // Only the quick emoji row holds emoji buttons without the input; its parents hold both.
            FilterRule(CommentsSettings.hideCommentsEmojiAndTimestampButtons, "comment_composer.e") {
                quickEmoji.check(it.elementBuffer).isFiltered &&
                    !composerControls.check(it.elementBuffer).isFiltered
            },
            FilterRule(
                CommentsSettings.hideCommentsPreviewComment,
                "|carousel_item",
                "comments_entry_point_teaser",
                "comments_entry_point_simplebox",
            ),
            FilterRule(CommentsSettings.hideCommentsThanksButton, "super_thanks_button.e"),
        )
        addTreeRewrite("video_metadata_carousel", ::withoutHiddenPages)
    }
}

// The comments card below a video lists a header (field 1) and a page (field 2) for each of its
// pages; both carry the page's kind.
private val CAROUSEL_PAGES = listOf(1, 168777401, 5, 408379376, 1)
private const val HEADER = 1
private const val HEADER_KIND = 7
private const val PAGE = 2
private const val PAGE_KIND = 15
private const val KIND_ASK = 3L
private const val KIND_HYPE = 11L

private fun withoutHiddenPages(tree: ByteArray): ByteArray {
    val hidden = buildSet {
        if (CommentsSettings.hideCommentsAskTeaser.value) add(KIND_ASK)
        if (CommentsSettings.hideCommentsHypeTeaser.value) add(KIND_HYPE)
    }
    if (hidden.isEmpty()) return tree
    return tree.rewriteAt(CAROUSEL_PAGES) { pages ->
        Proto.rewriteLength(pages) { number, entry ->
            val kind = when (number) {
                HEADER -> Proto.number(entry, HEADER_KIND, 0)
                PAGE -> Proto.number(entry, PAGE_KIND, 0)
                else -> return@rewriteLength entry
            }
            entry.takeUnless { kind in hidden }
        }
    }
}

private fun ByteArray.rewriteAt(path: List<Int>, rewrite: (ByteArray) -> ByteArray): ByteArray =
    if (path.isEmpty()) rewrite(this)
    else Proto.rewriteLength(this) { number, value ->
        if (number == path.first()) value.rewriteAt(path.drop(1), rewrite) else value
    }
