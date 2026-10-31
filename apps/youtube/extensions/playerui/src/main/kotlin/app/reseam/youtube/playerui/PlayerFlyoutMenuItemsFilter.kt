// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui

import app.reseam.runtime.Export
import app.reseam.youtube.litho.Filter
import app.reseam.youtube.litho.FilterGroup.ByteArrayFilterGroup
import app.reseam.youtube.litho.FilterGroupList.ByteArrayFilterGroupList
import app.reseam.youtube.litho.FilterRule
import app.reseam.youtube.litho.LithoComponent
import app.reseam.youtube.player.ShortsPlayer
import app.reseam.youtube.settings.PlayerOverlaySettings

@Export
class PlayerFlyoutMenuItemsFilter : Filter() {
    private val flyoutGroups = ByteArrayFilterGroupList()

    init {
        addPathCallbacks(
            FilterRule(
                PlayerOverlaySettings.hidePlayerFlyoutVideoQualityFooter,
                "quality_sheet_footer",
            ),
            FilterRule(null, "overflow_menu_item.e", accepts = ::hideFlyoutItem),
        )
        flyoutGroups.addAll(
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutCaptions,
                "closed_caption_",
                "yt_outline_experimental_closed_captions_",
            ),
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutListenWithYoutubeMusic,
                "yt_outline_youtube_music_",
                "yt_outline_experimental_youtube_music_",
            ),
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutHelp,
                "yt_outline_question_circle_",
                "yt_outline_experimental_help_circle_",
            ),
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutLockScreen,
                "yt_outline_lock_",
                "yt_outline_experimental_lock_",
            ),
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutSpeed,
                "yt_outline_play_arrow_half_circle_",
                "yt_outline_experimental_play_circle_half_dashed_",
            ),
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutAudioTrack,
                "yt_outline_person_radar_",
                "yt_outline_experimental_person_radar_",
            ),
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutAdditionalSettings,
                "yt_outline_gear_",
                "yt_outline_experimental_gear_",
            ),
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutAmbientMode,
                "yt_outline_screen_light_",
                "yt_outline_experimental_ambient_mode_",
            ),
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutLoopVideo,
                "yt_outline_arrow_repeat_1_",
                "yt_outline_experimental_repeat1_",
            ),
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutStableVolume,
                "volume_stable_",
                "yt_outline_experimental_stable_volume_",
            ),
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutSleepTimer,
                "yt_outline_moon_z_",
                "yt_outline_experimental_sleep_timer_",
            ),
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutWatchInVr,
                "yt_outline_vr_",
                "yt_outline_experimental_vr_",
            ),
            ByteArrayFilterGroup(
                PlayerOverlaySettings.hidePlayerFlyoutVideoQuality,
                "yt_outline_adjust_",
                "yt_outline_experimental_adjust_",
            ),
        )
    }

    private fun hideFlyoutItem(content: LithoComponent): Boolean =
        content.index == 0 &&
            !ShortsPlayer.isOpen &&
            flyoutGroups.check(content.buffer).isFiltered
}
