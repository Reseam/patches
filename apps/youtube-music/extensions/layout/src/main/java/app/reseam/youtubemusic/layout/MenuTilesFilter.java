// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtubemusic.layout;

import static app.reseam.youtubemusic.core.MusicSettings.toggle;

import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;
import app.reseam.youtube.litho.FilterGroupList.ByteArrayFilterGroupList;

/** The menu entries drawn with litho: the tiles above the list and the download row. */
public final class MenuTilesFilter extends Filter {
    private final StringFilterGroup tile = new StringFilterGroup(null, false, "|tile_button_inner.e");
    private final StringFilterGroup row = new StringFilterGroup(null, false, "list_item.e");
    private final ByteArrayFilterGroupList tileIcons = new ByteArrayFilterGroupList();
    private final ByteArrayFilterGroup download = icon("hide_menu_download", "_download_");

    public MenuTilesFilter() {
        addPathCallbacks(tile, row);
        tileIcons.addAll(
                icon("hide_menu_play_next", "_queue_next_"),
                icon("hide_menu_save_to_playlist", "_playlist_add_"),
                icon("hide_menu_share", "_share_"));
    }

    private static ByteArrayFilterGroup icon(String key, String name) {
        return new ByteArrayFilterGroup(key, toggle(key, false), name);
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        return matchedGroup == tile ? tileIcons.check(buffer).isFiltered()
                // The row's own buffer: deeper paths are its parts, which share it.
                : path.startsWith("list_item.e") && download.check(buffer).isFiltered();
    }
}
