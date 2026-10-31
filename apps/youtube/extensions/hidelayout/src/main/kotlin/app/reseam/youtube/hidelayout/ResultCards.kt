// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.hidelayout

import app.reseam.youtube.litho.FilterRule

/** A video, channel or Shorts card at the top of a feed or search result. */
internal val rootCards =
    FilterRule(
        null,
        "home_video_with_context.e",
        "search_video_with_context.e",
        "video_with_context.e",
        "related_video_with_context.e",
        "video_lockup_with_attachment.e",
        "compact_video.e",
        "inline_shorts",
        "shorts_video_cell",
        "shorts_pivot_item.e",
    ) {
        it.index == 0
    }
