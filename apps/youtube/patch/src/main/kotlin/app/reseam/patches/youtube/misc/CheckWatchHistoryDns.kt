// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.misc

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.mainActivityOnCreate
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.misc.bindings.WatchHistoryDns
import app.reseam.youtube.settings.AdvancedSettings
import app.reseam.youtube.settings.YouTubeStrings

val checkWatchHistoryDns = patch(YouTubePatchStrings.checkWatchHistoryDns) {
    description(YouTubePatchStrings.checkWatchHistoryDnsDescription)
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Advanced, YouTubeStrings.sectionWatchHistory, AdvancedSettings.checkWatchHistoryDns))

    execute {
        // The check needs an Activity to put a dialog on, and it is cheap enough to run at launch.
        gate(AdvancedSettings.checkWatchHistoryDns) {
            mainActivityOnCreate.after { WatchHistoryDns.check(thisObject) }
        }
    }
}
