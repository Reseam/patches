// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.x.withheld

internal enum class ProfileTab(val operation: String, val withReplies: Boolean) {
    Posts("UserProfileOriginalsTimelineQuery", false),
    Replies("UserProfileRepliesTimelineQuery", true),
    NewReplies("UserProfileNewRepliesTimelineQuery", true),
    Media("UserProfileMediaTimelineQuery", false),
    Photos("UserProfilePhotosTimelineQuery", false),
    Videos("UserProfileVideosTimelineQuery", false),
    ;

    fun includes(status: FxStatus): Boolean =
        !status.isRepost &&
            when (this) {
                Posts -> status.replyTo == null
                Replies, NewReplies -> true
                Media -> status.media.isNotEmpty()
                Photos -> status.media.any { it is FxMedia.Photo }
                Videos -> status.media.any { it is FxMedia.Video }
            }

    companion object {
        fun of(operation: String): ProfileTab? = entries.firstOrNull { it.operation == operation }
    }
}
