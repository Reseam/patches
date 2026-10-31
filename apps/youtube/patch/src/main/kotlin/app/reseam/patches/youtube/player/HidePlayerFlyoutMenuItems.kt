// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.playerTypeHook
import app.reseam.patches.youtubecommon.lithoFilter
import app.reseam.patches.youtubecommon.registerLithoFilter
import app.reseam.youtube.playerui.bindings.PlayerFlyoutMenuItemsFilter
import app.reseam.youtube.settings.PlayerOverlaySettings
import app.reseam.youtube.settings.YouTubeStrings

val hidePlayerFlyoutMenuItems = patch("Hide player flyout menu items") {
    description("Hides selected items from the player's overflow menu.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, lithoFilter, playerTypeHook)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Overlay,
            YouTubeStrings.sectionPlayerFlyoutMenu,
            PlayerOverlaySettings.hidePlayerFlyoutCaptions,
            PlayerOverlaySettings.hidePlayerFlyoutListenWithYoutubeMusic,
            PlayerOverlaySettings.hidePlayerFlyoutHelp,
            PlayerOverlaySettings.hidePlayerFlyoutLockScreen,
            PlayerOverlaySettings.hidePlayerFlyoutSpeed,
            PlayerOverlaySettings.hidePlayerFlyoutAudioTrack,
            PlayerOverlaySettings.hidePlayerFlyoutAdditionalSettings,
            PlayerOverlaySettings.hidePlayerFlyoutAmbientMode,
            PlayerOverlaySettings.hidePlayerFlyoutLoopVideo,
            PlayerOverlaySettings.hidePlayerFlyoutStableVolume,
            PlayerOverlaySettings.hidePlayerFlyoutSleepTimer,
            PlayerOverlaySettings.hidePlayerFlyoutWatchInVr,
            PlayerOverlaySettings.hidePlayerFlyoutVideoQuality,
            PlayerOverlaySettings.hidePlayerFlyoutVideoQualityFooter,
        ),
    )

    execute { registerLithoFilter(PlayerFlyoutMenuItemsFilter) }
}
