// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.youtube.settings

import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object ChannelSettings : SettingsGroup(YouTubeStrings, YOUTUBE_KEYS) {
    val hideChannelTab by toggle(default = false)
    val hideChannelTabFilterStrings by text(default = "", multiline = true)
    val hideCommunityButton by toggle(default = true)
    val hideJoinButton by toggle(default = false)
    val hideLinksPreview by toggle(default = true)
    val hideMembersShelf by toggle(default = true)
    val hideStoreButton by toggle(default = true)
    val hideSubscribeButtonInChannelPage by toggle(default = false)
    val searchInsideChannels by toggle(default = true)
}
