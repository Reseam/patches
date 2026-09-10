// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.core

import app.reseam.patch.settings.folder
import app.reseam.patch.settings.toggle

object GhostSettings {
    val hideTyping by toggle("Hide typing indicator", default = true)
    val hideDmSeen by toggle("Hide DM read receipts", default = true)
    val hideStorySeen by toggle("Hide story views", default = true)
    val hideLiveSeen by toggle("Hide live views", default = true)
    val hideScreenshotNotifications by toggle("Hide screenshot notifications", default = true)
}

object MediaSettings {
    val maxResolution by toggle("Max photo resolution", default = true)
}

object PlaybackSettings {
    val disableVideoAutoplay by toggle("Disable video autoplay", default = true)
}

object DeveloperSettings {
    val unlockDeveloperOptions by toggle("Unlock developer options", default = true)
}

object FollowSettings {
    val followsYouIndicator by toggle("Follows-you indicator", default = false)
}

object DownloadSettings {
    val folder by folder("Download folder", default = "ReseamInsta")
    val showToast by toggle("Show download toast", default = true)
}
