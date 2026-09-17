// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.ExtClass
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.lithoFilter
import app.reseam.patches.youtube.internal.playerTypeHook
import app.reseam.patches.youtube.internal.registerLithoFilter

val hidePlayerFlyoutMenuItems = patch("Hide player flyout menu items") {
    description("Hides selected items from the player's overflow menu.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings, lithoFilter, playerTypeHook)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Overlay,
            "Player flyout menu",
            YouTubeSettings.hidePlayerFlyoutCaptions,
            YouTubeSettings.hidePlayerFlyoutListenWithYoutubeMusic,
            YouTubeSettings.hidePlayerFlyoutHelp,
            YouTubeSettings.hidePlayerFlyoutLockScreen,
            YouTubeSettings.hidePlayerFlyoutSpeed,
            YouTubeSettings.hidePlayerFlyoutAudioTrack,
            YouTubeSettings.hidePlayerFlyoutAdditionalSettings,
            YouTubeSettings.hidePlayerFlyoutAmbientMode,
            YouTubeSettings.hidePlayerFlyoutLoopVideo,
            YouTubeSettings.hidePlayerFlyoutStableVolume,
            YouTubeSettings.hidePlayerFlyoutSleepTimer,
            YouTubeSettings.hidePlayerFlyoutWatchInVr,
            YouTubeSettings.hidePlayerFlyoutVideoQuality,
            YouTubeSettings.hidePlayerFlyoutVideoQualityFooter,
        ),
    )

    execute { registerLithoFilter(PlayerFlyoutMenuItemsFilter) }
}

object PlayerFlyoutMenuItemsFilter : ExtClass("app.reseam.youtube.playerui.PlayerFlyoutMenuItemsFilter")
