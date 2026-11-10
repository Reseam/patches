// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.literal
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.youtube.playerui.bindings.CustomPlayerOverlayOpacity
import app.reseam.youtube.settings.PlayerOverlaySettings
import app.reseam.youtube.settings.YouTubeStrings

val customPlayerOverlayOpacity = patch(YouTubePatchStrings.customPlayerOverlayOpacity) {
    description(YouTubePatchStrings.customPlayerOverlayOpacityDescription)
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, YouTubeStrings.sectionPlayerOverlay, PlayerOverlaySettings.playerOverlayOpacity))

    execute {
        val scrim = resources.id("id", "scrim_overlay")?.toLong()
            ?: error("id/scrim_overlay is missing")
        method("player scrim overlay") {
            returns(Type.Void)
            literals(scrim)
        }.point("scrim lookup") {
            invokeVirtual { name("findViewById"); params(Type.Int); returns(Type.View) }
            argument(1) { literal(scrim) }
        }.next { resultOf(Type.View) }.captureAs("scrim", Type.View).after {
            CustomPlayerOverlayOpacity.applyOpacity(capture("scrim").cast("android.widget.ImageView"))
        }
    }
}
