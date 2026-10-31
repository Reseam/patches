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
class MenuTilesFilter : Filter() {
    private val tileIcons =
        ByteArrayFilterGroupList().apply {
            addAll(
                ByteArrayFilterGroup(YouTubeMusicSettings.hideMenuPlayNext, "_queue_next_"),
                ByteArrayFilterGroup(YouTubeMusicSettings.hideMenuSaveToPlaylist, "_playlist_add_"),
                ByteArrayFilterGroup(YouTubeMusicSettings.hideMenuShare, "_share_"),
            )
        }
    private val download = ByteArrayFilterGroup(YouTubeMusicSettings.hideMenuDownload, "_download_")

    init {
        addPathCallbacks(
            FilterRule(null, "|tile_button_inner.e") { tileIcons.check(it.buffer).isFiltered },
            // Deeper paths are parts of the row and share its buffer.
            FilterRule(null, "list_item.e") {
                it.path.startsWith("list_item.e") && download.check(it.buffer).isFiltered
            },
        )
    }
}
