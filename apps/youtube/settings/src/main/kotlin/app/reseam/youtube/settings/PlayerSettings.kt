// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object PlayerSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val disableSignInToTvPopup by toggle(default = false)
    val disableAmbientMode by toggle(default = false)
    val disableFullscreenAmbientMode by toggle(default = true)
    val hideChannelBar by toggle(default = false)
    val hideChannelWatermark by toggle(default = true)
    val hideFullscreenQuickActions by toggle(default = false)
    val hideInfoPanels by toggle(default = true)
    val hideJoinMembershipButton by toggle(default = true)
    val hideMoreVideosButton by toggle(default = false)
    val hideNotifyMeButton by toggle(default = true)
    val hideSubscribersCommunityGuidelines by toggle(default = true)
    val hideUploadTime by toggle(default = false)
    val hideVideoTitle by toggle(default = false)
    val openVideosFullscreen by toggle(default = false)
    val hideMiniplayerOverlayButtons by toggle(default = false)
    val hideMiniplayerSubtext by toggle(default = false)
    val miniplayerWidthDip by text(default = "192")
    val compactMiniplayer by toggle(default = false)
    val loopVideo by toggle(default = false)
    val loopVideoButton by toggle(default = false)
}
