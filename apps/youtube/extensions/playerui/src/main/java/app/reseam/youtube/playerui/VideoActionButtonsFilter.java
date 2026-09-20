// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui;

import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;
import app.reseam.youtube.litho.FilterGroupList.StringFilterGroupList;

public final class VideoActionButtonsFilter extends Filter {
    private static final String VIDEO_ACTION_BAR_PATH_PREFIX = "video_action_bar.e";
    private static final String COMPACT_CHANNEL_BAR_PATH_PREFIX = "compact_channel_bar.e";
    private static final String COMPACTIFY_VIDEO_ACTION_BAR_PATH = "compactify_video_action_bar.e";

    private final StringFilterGroup actionBarGroup;
    private final StringFilterGroup buttonFilterPathGroup;
    private final StringFilterGroup likeSubscribeGlow;
    private final StringFilterGroupList accessibilityGroups = new StringFilterGroupList();

    public VideoActionButtonsFilter() {
        actionBarGroup = new StringFilterGroup(null, false, "video_action_bar.e");
        addIdentifierCallbacks(actionBarGroup);

        likeSubscribeGlow = new StringFilterGroup(
                "disable_like_subscribe_glow", false, "animated_button_border.e");
        buttonFilterPathGroup = new StringFilterGroup(null, false, "|ContainerType|button.e");
        addPathCallbacks(
                likeSubscribeGlow,
                // Like and dislike are separate components with their own paths; the combined
                // segmented_like_dislike_button is not built on this release.
                new StringFilterGroup("hide_like_dislike_button", false,
                        "|like_button_with_vm_input.e", "|dislike_button_vm.e"),
                new StringFilterGroup("hide_download_button", false, "|download_button.e"),
                new StringFilterGroup("hide_save_button", false, "|save_to_playlist_button"),
                buttonFilterPathGroup);

        accessibilityGroups.addAll(
                new StringFilterGroup("hide_share_button", false, "id.video.share.button"),
                new StringFilterGroup("hide_remix_button", false, "id.video.remix.button"));
    }

    private boolean everyFilterGroupEnabled() {
        for (StringFilterGroup group : pathCallbacks) {
            if (!group.isEnabled()) return false;
        }
        for (StringFilterGroup group : accessibilityGroups) {
            if (!group.isEnabled()) return false;
        }
        return true;
    }

    private boolean hideButtons(String path, String accessibility) {
        if (!path.startsWith(VIDEO_ACTION_BAR_PATH_PREFIX)
                && !path.startsWith(COMPACTIFY_VIDEO_ACTION_BAR_PATH)) return false;
        return accessibilityGroups.check(accessibility).isFiltered();
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == likeSubscribeGlow) {
            return path.startsWith(VIDEO_ACTION_BAR_PATH_PREFIX)
                    || path.startsWith(COMPACT_CHANNEL_BAR_PATH_PREFIX)
                    || path.startsWith(COMPACTIFY_VIDEO_ACTION_BAR_PATH);
        }
        if (matchedGroup == actionBarGroup) return everyFilterGroupEnabled();
        if (matchedGroup == buttonFilterPathGroup) return hideButtons(path, accessibility);
        return true;
    }
}
