// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.sponsorblock

import android.app.AlertDialog
import android.content.Context
import app.reseam.youtube.core.showToast
import app.reseam.youtube.settings.YouTubeStrings

internal fun showSponsorBlockSegments(context: Context) {
    val segments = SponsorBlockSession.segments
    val labels =
        segments
            .map {
                YouTubeStrings.sponsorblockSegmentLabel(
                    it.category.title,
                    formatSponsorBlockDuration(it.start / 1000),
                    formatSponsorBlockDuration(it.end / 1000),
                )
            }
            .toTypedArray()
    if (labels.isEmpty()) {
        showToast(YouTubeStrings.sponsorblockNoSegments)
        return
    }
    AlertDialog.Builder(context)
        .setTitle(YouTubeStrings.sponsorblockSegments)
        .setItems(labels) { _, which ->
            val segment = segments[which]
            val actions =
                listOf(
                    SponsorBlockAction(YouTubeStrings.sponsorblockJumpToStart) {
                        SponsorBlockSkips.undo(segment, segment.start)
                    },
                    SponsorBlockAction(YouTubeStrings.sponsorblockUpvote) {
                        vote(segment, "type" to "1")
                    },
                    SponsorBlockAction(YouTubeStrings.sponsorblockDownvote) {
                        vote(segment, "type" to "0")
                    },
                    SponsorBlockAction(YouTubeStrings.sponsorblockChangeCategory) {
                        chooseSponsorBlockCategory(context) { vote(segment, "category" to it.key) }
                    },
                )
            AlertDialog.Builder(context).setTitle(labels[which]).setActions(actions).show()
        }
        .show()
}

private fun vote(segment: Segment, query: Pair<String, String>) {
    executeSponsorBlockRequest {
        try {
            requestSponsorBlock(
                "POST",
                "/api/voteOnSponsorTime",
                "userID" to sponsorBlockUserId(),
                "UUID" to segment.uuid,
                query,
            )
            showToast(YouTubeStrings.sponsorblockVoteSubmitted)
        } catch (error: Exception) {
            showToast(YouTubeStrings.sponsorblockVoteFailed(error.message.toString()))
        }
    }
}

internal fun chooseSponsorBlockCategory(context: Context, callback: (Segment.Category) -> Unit) {
    val categories = Segment.Category.entries
    AlertDialog.Builder(context)
        .setTitle(YouTubeStrings.sponsorblockCategory)
        .setItems(categories.map { it.title }.toTypedArray()) { _, which ->
            callback(categories[which])
        }
        .show()
}
