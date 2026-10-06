// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtubemusic.layout;

import static app.reseam.youtubemusic.core.MusicSettings.toggle;

import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;
import app.reseam.youtube.litho.FilterGroupList.ByteArrayFilterGroupList;

/**
 * The action bar below the player. Like and download have their own components; the other buttons
 * share one and differ only by icon, which only the inner part's own buffer names.
 */
public final class ActionButtonsFilter extends Filter {
    private static final String ACTION_BAR = "video_action_bar.e";

    private final StringFilterGroup actionBar = new StringFilterGroup(null, false, ACTION_BAR);
    private final StringFilterGroup iconButton = new StringFilterGroup(null, false, "|button_inner.e");
    private final ByteArrayFilterGroupList icons = new ByteArrayFilterGroupList();

    public ActionButtonsFilter() {
        addIdentifierCallbacks(actionBar);
        addPathCallbacks(
                group("hide_like_dislike_button", "|segmented_like_dislike_button.e"),
                group("hide_download_button", "|music_download_button.e"),
                iconButton);
        // Icon names without their theme prefix, which the server switches between experiments.
        icons.addAll(
                icon("hide_comments_button", "_text_bubble_"),
                icon("hide_save_button", "_playlist_add_"),
                icon("hide_share_button", "_share_"),
                icon("hide_lyrics_button", "_quote_"),
                icon("hide_mix_button", "_mix_"));
    }

    private static StringFilterGroup group(String key, String path) {
        return new StringFilterGroup(key, toggle(key, false), path);
    }

    private static ByteArrayFilterGroup icon(String key, String name) {
        return new ByteArrayFilterGroup(key, toggle(key, false), name);
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == actionBar) return allHidden();
        if (!path.startsWith(ACTION_BAR)) return false;
        return matchedGroup != iconButton || icons.check(buffer).isFiltered();
    }

    private boolean allHidden() {
        for (StringFilterGroup group : pathCallbacks) {
            if (group != iconButton && !group.isEnabled()) return false;
        }
        for (ByteArrayFilterGroup group : icons) {
            if (!group.isEnabled()) return false;
        }
        return true;
    }
}
