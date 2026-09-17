// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui;

import app.reseam.youtube.core.Logger;
import app.reseam.youtube.litho.Filter;
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup;
import app.reseam.youtube.litho.FilterGroup.StringFilterGroup;
import app.reseam.youtube.litho.FilterGroupList.ByteArrayFilterGroupList;
import app.reseam.youtube.player.ShortsPlayerState;

public final class PlayerFlyoutMenuItemsFilter extends Filter {
    private final ByteArrayFilterGroupList flyoutGroups = new ByteArrayFilterGroupList();
    private final StringFilterGroup videoQualityFooter;

    public PlayerFlyoutMenuItemsFilter() {
        videoQualityFooter = new StringFilterGroup(
                "hide_player_flyout_video_quality_footer", false, "quality_sheet_footer");

        addPathCallbacks(videoQualityFooter, new StringFilterGroup(null, false, "overflow_menu_item.e"));
        flyoutGroups.addAll(
                new ByteArrayFilterGroup("hide_player_flyout_captions", false,
                        "closed_caption_", "yt_outline_experimental_closed_captions_"),
                new ByteArrayFilterGroup("hide_player_flyout_listen_with_youtube_music", false,
                        "yt_outline_youtube_music_", "yt_outline_experimental_youtube_music_"),
                new ByteArrayFilterGroup("hide_player_flyout_help", true,
                        "yt_outline_question_circle_", "yt_outline_experimental_help_circle_"),
                new ByteArrayFilterGroup("hide_player_flyout_lock_screen", false,
                        "yt_outline_lock_", "yt_outline_experimental_lock_"),
                new ByteArrayFilterGroup("hide_player_flyout_speed", false,
                        "yt_outline_play_arrow_half_circle_", "yt_outline_experimental_play_circle_half_dashed_"),
                new ByteArrayFilterGroup("hide_player_flyout_audio_track", false,
                        "yt_outline_person_radar_", "yt_outline_experimental_person_radar_"),
                new ByteArrayFilterGroup("hide_player_flyout_additional_settings", false,
                        "yt_outline_gear_", "yt_outline_experimental_gear_"),
                new ByteArrayFilterGroup("hide_player_flyout_ambient_mode", false,
                        "yt_outline_screen_light_", "yt_outline_experimental_ambient_mode_"),
                new ByteArrayFilterGroup("hide_player_flyout_loop_video", false,
                        "yt_outline_arrow_repeat_1_", "yt_outline_experimental_repeat1_"),
                new ByteArrayFilterGroup("hide_player_flyout_stable_volume", false,
                        "volume_stable_", "yt_outline_experimental_stable_volume_"),
                new ByteArrayFilterGroup("hide_player_flyout_sleep_timer", false,
                        "yt_outline_moon_z_", "yt_outline_experimental_sleep_timer_"),
                new ByteArrayFilterGroup("hide_player_flyout_watch_in_vr", false,
                        "yt_outline_vr_", "yt_outline_experimental_vr_"),
                new ByteArrayFilterGroup("hide_player_flyout_video_quality", false,
                        "yt_outline_adjust_", "yt_outline_experimental_adjust_"));
        Logger.debug(() -> "PlayerFlyoutMenuItemsFilter: 14 toggle groups registered");
    }

    @Override
    public boolean isFiltered(String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == videoQualityFooter) return true;
        if (contentIndex != 0 || ShortsPlayerState.isOpen()) return false;
        return flyoutGroups.check(buffer).isFiltered();
    }
}
