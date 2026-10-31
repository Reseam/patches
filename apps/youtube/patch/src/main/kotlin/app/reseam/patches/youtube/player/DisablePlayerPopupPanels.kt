// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.engagementPanelShow
import app.reseam.youtube.settings.PlayerOverlaySettings
import app.reseam.youtube.settings.YouTubeStrings

val disablePlayerPopupPanels = patch("Disable player popup panels") {
    description("Stops panels such as live chat from opening by themselves over the player.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, YouTubeStrings.sectionPlayerOverlay, PlayerOverlaySettings.disablePlayerPopupPanels))

    execute {
        gate(PlayerOverlaySettings.disablePlayerPopupPanels) {
            // The last flag is set only when the player opens a panel on its own; taps pass false.
            engagementPanelShow.before {
                whenTrue(param(3)) { returnNull() }
            }
        }
    }
}
