// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.settings.PlayerOverlaySettings
import app.reseam.youtube.settings.YouTubeStrings

val hideTimestamp = patch(YouTubePatchStrings.hideTimestamp) {
    description(YouTubePatchStrings.hideTimestampDescription)
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, YouTubeStrings.sectionPlayerOverlay, PlayerOverlaySettings.hideTimestamp))

    execute {
        gate(PlayerOverlaySettings.hideTimestamp) {
            method("timestamp update") {
                flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
                params()
                returns(Type.Void)
                calls { params(Type.Long); returns(Type.CharSequence) }
                calls { params(Type.CharSequence, Type.CharSequence, Type.CharSequence); returns(Type.Void) }
            }.before { returnVoid() }
        }
    }
}
