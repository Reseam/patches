// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later


package app.reseam.patches.instagram.core

import app.reseam.patch.settings.FolderSetting
import app.reseam.patch.settings.ToggleSetting

object GhostSettings {
    val HideTyping = ToggleSetting(
        key = "ghost.hide_typing",
        title = "Hide typing indicator",
        default = true,
    )

    val HideDmSeen = ToggleSetting(
        key = "ghost.hide_dm_seen",
        title = "Hide DM read receipts",
        default = true,
    )

    val HideStorySeen = ToggleSetting(
        key = "ghost.hide_story_seen",
        title = "Hide story views",
        default = true,
    )

    val HideLiveSeen = ToggleSetting(
        key = "ghost.hide_live_seen",
        title = "Hide live views",
        default = true,
    )

    val HideScreenshotNotifications = ToggleSetting(
        key = "ghost.hide_screenshot_notifications",
        title = "Hide screenshot notifications",
        default = true,
    )
}

object MediaSettings {
    val MaxResolution = ToggleSetting(
        key = "media.max_resolution",
        title = "Max photo resolution",
        default = true,
    )
}

object MediaPlaybackSettings {
    val DisableVideoAutoplay = ToggleSetting(
        key = "playback.disable_video_autoplay",
        title = "Disable video autoplay",
        default = true,
    )
}

object DeveloperSettings {
    val UnlockDeveloperOptions = ToggleSetting(
        key = "developer.unlock_options",
        title = "Unlock developer options",
        default = true,
    )
}

object FollowSettings {
    val FollowsYouIndicator = ToggleSetting(
        key = "follow.follows_you_indicator",
        title = "Follows-you indicator",
        default = false,
    )
}

object DownloadSettings {
    val Folder = FolderSetting(
        key = "download.folder",
        title = "Download folder",
        default = "ReseamInsta",
    )

    val ShowToast = ToggleSetting(
        key = "download.show_toast",
        title = "Show download toast",
        default = true,
    )
}
