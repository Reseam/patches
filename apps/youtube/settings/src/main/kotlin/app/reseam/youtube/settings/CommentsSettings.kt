// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.toggle

object CommentsSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val hideCommentsAiChatSummary by toggle(default = false)
    val hideCommentsAiSummary by toggle(default = false)
    val hideCommentsByMembersHeader by toggle(default = false)
    val hideCommentsChannelGuidelines by toggle(default = true)
    val hideCommentsCommunityGuidelines by toggle(default = true)
    val hideCommentsCreateAShortButton by toggle(default = true)
    val hideCommentsEmojiAndTimestampButtons by toggle(default = false)
    val hideCommentsPreviewComment by toggle(default = false)
    val hideCommentsSection by toggle(default = false)
    val hideCommentsSectionInHomeFeed by toggle(default = false)
    val hideCommentsThanksButton by toggle(default = true)
}
