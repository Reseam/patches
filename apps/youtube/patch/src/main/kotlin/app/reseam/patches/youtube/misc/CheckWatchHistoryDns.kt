// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.patch
import app.reseam.patch.settings.after
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.core.youTubeSettings

val checkWatchHistoryDns = patch("Check watch history domain name resolution") {
    description("Warns at launch when the device DNS server blocks the domain that records watch history.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, "Watch history", YouTubeSettings.checkWatchHistoryDns))

    execute {
        // The check needs an Activity to put a dialog on, and it is cheap enough to run at launch.
        mainActivityOnCreate.after(YouTubeSettings.checkWatchHistoryDns) { call(WatchHistoryDns.check, thisObject) }
    }
}


object WatchHistoryDns : ExtClass("app.reseam.youtube.misc.WatchHistoryDns") {
    val check = static("check", Type.Activity)
}
