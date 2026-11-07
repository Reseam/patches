// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object PlayerOverlaySettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val hideAutoplayButton by toggle(default = true)
    val hideCaptionsButton by toggle(default = false)
    val hideCastButton by toggle(default = false)
    val hideCollapseButton by toggle(default = false)
    val hideFullscreenButton by toggle(default = false)
    val hidePlayerControlButtonsBackground by toggle(default = false)
    val hidePlayerPreviousNextButtons by toggle(default = false)
    val hideAutoplayPreview by toggle(default = false)
    val hideEndScreenCards by toggle(default = false)
    val hideEndScreenSuggestedVideo by toggle(default = false)
    val hideRelatedVideoOverlay by toggle(default = false)
    val hideTimestamp by toggle(default = false)
    val disablePlayerPopupPanels by toggle(default = false)
    val disableRollingNumberAnimations by toggle(default = false)
    val playPauseButtonSize by text(default = "56")
    val playerOverlayOpacity by text(default = "100")
    val disableAutoCaptions by toggle(default = false)
    val disableLikeSubscribeGlow by toggle(default = false)
    val hideLikeDislikeButton by toggle(default = false)
    val hideDownloadButton by toggle(default = false)
    val hideAskButton by toggle(default = false)
    val hideRemixButton by toggle(default = false)
    val hideSaveButton by toggle(default = false)
    val hideShareButton by toggle(default = false)
    val hideInfoCards by toggle(default = false)
    val hidePlayerFlyoutCaptions by toggle(default = false)
    val hidePlayerFlyoutListenWithYoutubeMusic by toggle(default = false)
    val hidePlayerFlyoutHelp by toggle(default = true)
    val hidePlayerFlyoutLockScreen by toggle(default = false)
    val hidePlayerFlyoutSpeed by toggle(default = false)
    val hidePlayerFlyoutAudioTrack by toggle(default = false)
    val hidePlayerFlyoutAdditionalSettings by toggle(default = false)
    val hidePlayerFlyoutAmbientMode by toggle(default = false)
    val hidePlayerFlyoutLoopVideo by toggle(default = false)
    val hidePlayerFlyoutStableVolume by toggle(default = false)
    val hidePlayerFlyoutSleepTimer by toggle(default = false)
    val hidePlayerFlyoutWatchInVr by toggle(default = false)
    val hidePlayerFlyoutVideoQuality by toggle(default = false)
    val hidePlayerFlyoutVideoQualityFooter by toggle(default = false)
}
