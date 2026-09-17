// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.Type
import app.reseam.patch.classTarget
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val hideRelatedVideoOverlay = patch("Hide related-video overlay") {
    description("Removes related videos shown over the player.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, "End screen", YouTubeSettings.hideRelatedVideoOverlay))

    execute {
        val parent = method("related-video layout parent") {
            returns(Type.Void)
            literals(resources.id("layout", "app_related_endscreen_results")?.toLong()
                ?: error("layout/app_related_endscreen_results is missing"))
        }
        val owner = classTarget("related-video layout class") {
            bytecode.findClass(parent.owner) ?: error("The related-video layout class is missing")
        }
        // The app has one three-argument related-results renderer in this owner; all three
        // arguments are stable even though the renderer's name is obfuscated.
        val renderer = method("related-video renderer") {
            inClass(owner)
            params(Type.Int, Type.Boolean, Type.Int)
            returns(Type.Void)
        }
        renderer.before(YouTubeSettings.hideRelatedVideoOverlay) { returnVoid() }
    }
}
