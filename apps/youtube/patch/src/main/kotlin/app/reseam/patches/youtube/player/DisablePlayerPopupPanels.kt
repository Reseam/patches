// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.Type
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.engagementPanelShow

val disablePlayerPopupPanels = patch("Disable player popup panels") {
    description("Stops popup panels from opening from the player.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, "Player overlay", YouTubeSettings.disablePlayerPopupPanels))

    execute {
        engagementPanelShow.before(YouTubeSettings.disablePlayerPopupPanels) {
            whenFalse(param(3)) { returnNull() }
        }
    }
}
