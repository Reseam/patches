// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.player

import app.reseam.patch.Type
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.after
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

val hideEndScreenCards = patch("Hide end screen cards") {
    description("Removes cards shown over the video at the end of playback.")
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)
    settings(youTubeSettings, section(YouTubeSettingsPages.Overlay, "End screen", YouTubeSettings.hideEndScreenCards))

    execute {
        method("creator end-screen initialization") {
            strings("Error creating creator EndscreenElement, ignoring it. Style: %s")
        }.before(YouTubeSettings.hideEndScreenCards) { returnVoid() }

        fun resourceId(name: String) = resources.id("layout", "endscreen_element_layout_$name")?.toLong()
            ?: error("layout/endscreen_element_layout_$name is missing")

        listOf("circle", "icon", "video").forEach { name ->
            val id = resourceId(name)
            method("end-screen $name layout") {
                params()
                returns(Type.View)
                literals(id)
            }.after(YouTubeSettings.hideEndScreenCards) {
                whenNotNull(capture("result")) {
                    capture("result").callVirtual(Type.View, "setVisibility", "(I)V", int(8))
                }
            }
        }

    }
}
