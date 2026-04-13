/*
 * Stitch — part of the Stitch Android patcher.
 * Copyright (C) 2026 Aunali321 <accounts@auna.li>
 *
 * This file is part of Stitch.
 *
 * Stitch is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stitch is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.stitch.patches.instagram.core

import dev.stitch.patch.settings.FolderSetting
import dev.stitch.patch.settings.ToggleSetting

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
        default = "StitchInsta",
    )

    val ShowToast = ToggleSetting(
        key = "download.show_toast",
        title = "Show download toast",
        default = true,
    )
}
