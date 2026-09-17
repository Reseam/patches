// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.literal
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val customPlayerOverlayOpacity = patch("Custom player overlay opacity") {
    description("Changes the opacity of the player background while controls are visible.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, "Player overlay", YouTubeSettings.playerOverlayOpacity))

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
            call(CustomPlayerOverlayOpacity.changeOpacity, capture("scrim").cast("android.widget.ImageView"))
        }
    }
}

object CustomPlayerOverlayOpacity : ExtClass("app.reseam.youtube.playerui.CustomPlayerOverlayOpacity") {
    val changeOpacity = static("changeOpacity", "android.widget.ImageView")
}
