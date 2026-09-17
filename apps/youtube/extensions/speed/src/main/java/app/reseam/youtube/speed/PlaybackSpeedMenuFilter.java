// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.speed;

import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.Filter.FilterContentType;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;

/** Notes which Litho speed menu YouTube is building, so {@link CustomPlaybackSpeed} can replace it. */
public final class PlaybackSpeedMenuFilter extends Filter {
    /** The old Litho speed menu. */
    public static volatile boolean oldPlaybackSpeedMenuVisible;
    /** The 0.05x speed selection menu. */
    public static volatile boolean playbackRateSelectorMenuVisible;

    private final StringFilterGroup oldPlaybackMenu;

    public PlaybackSpeedMenuFilter() {
        StringFilterGroup playbackRateSelector = new StringFilterGroup(
                "custom_playback_speed_menu", true, "playback_rate_selector_menu_sheet.e");
        oldPlaybackMenu = new StringFilterGroup(
                "custom_playback_speed_menu", true, "playback_speed_sheet_content.e");
        addPathCallbacks(playbackRateSelector, oldPlaybackMenu);
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType,
                              int contentIndex) {
        if (matchedGroup == oldPlaybackMenu) oldPlaybackSpeedMenuVisible = true;
        else playbackRateSelectorMenuVisible = true;
        return false;
    }
}
