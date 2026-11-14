// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.youtube.playerui

import app.reseam.runtime.Export
import app.reseam.runtime.ToggleValue
import app.reseam.youtube.core.Proto
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
            row(
                PlayerOverlaySettings.hidePlayerFlyoutCaptions,
                "closed_caption_",
                "experimental_closed_captions_",
                iconType = 20,
            ),
            row(
                PlayerOverlaySettings.hidePlayerFlyoutListenWithYoutubeMusic,
                "yt_outline_youtube_music_",
                "yt_outline_experimental_youtube_music_",
            ),
            row(
                PlayerOverlaySettings.hidePlayerFlyoutHelp,
                "yt_outline_question_circle_",
                "yt_outline_experimental_help_circle_",
                iconType = 181,
            ),
            row(
                PlayerOverlaySettings.hidePlayerFlyoutLockScreen,
                "yt_outline_lock_",
                "yt_outline_experimental_lock_",
                iconType = 198,
            ),
            row(
                PlayerOverlaySettings.hidePlayerFlyoutSpeed,
                "yt_outline_play_arrow_half_circle_",
                "yt_outline_experimental_play_circle_half_dashed_",
                iconType = 231,
            ),
            row(
                PlayerOverlaySettings.hidePlayerFlyoutAudioTrack,
                "yt_outline_person_radar_",
                "yt_outline_experimental_person_radar_",
                "experimental_person_waves_",
            ),
            row(
                PlayerOverlaySettings.hidePlayerFlyoutAdditionalSettings,
                "yt_outline_gear_",
                "yt_outline_experimental_gear_",
                iconType = 173,
            ),
            row(
                PlayerOverlaySettings.hidePlayerFlyoutAmbientMode,
                "yt_outline_screen_light_",
                "yt_outline_experimental_ambient_mode_",
                iconType = 737,
            ),
            row(
                PlayerOverlaySettings.hidePlayerFlyoutLoopVideo,
                "yt_outline_arrow_repeat_1_",
                "yt_outline_experimental_repeat1_",
                iconType = 444,
            ),
            row(
                PlayerOverlaySettings.hidePlayerFlyoutStableVolume,
                "volume_stable_",
                "experimental_stable_volume_",
                iconType = 676,
            ),
            row(
                PlayerOverlaySettings.hidePlayerFlyoutSleepTimer,
                "yt_outline_moon_z_",
                "yt_outline_experimental_sleep_timer_",
                iconType = 331,
            ),
            row(PlayerOverlaySettings.hidePlayerFlyoutWatchInVr, "yt_outline_vr_", "yt_outline_experimental_vr_"),
            row(
                PlayerOverlaySettings.hidePlayerFlyoutVideoQuality,
                "yt_outline_adjust_",
                "yt_outline_experimental_adjust_",
                iconType = 89,
            ),
            row(PlayerOverlaySettings.hidePlayerFlyoutOnTheGo, iconType = 179),
        )
    }

    // A row's icon comes either by name or as a type in the image element's field 1002 { 1 { 1 } },
    // which one varies per row and response.
    private fun row(setting: ToggleValue, vararg icons: String, iconType: Int? = null) =
        ByteArrayFilterGroup(
            setting,
            *(icons.map(String::encodeToByteArray) + listOfNotNull(iconType?.let(::iconTypePattern))).toTypedArray(),
        )

    private fun iconTypePattern(type: Int): ByteArray =
        Proto.Writer().message(1002, Proto.Writer().message(1, Proto.Writer().number(1, type.toLong()))).toByteArray()

    private fun hideFlyoutItem(content: LithoComponent): Boolean =
        content.index == 0 &&
            !ShortsPlayer.isOpen &&
            flyoutGroups.check(content.buffer).isFiltered
}
