// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.stories

import app.reseam.instagram.settings.InstagramStrings
import app.reseam.instagram.settings.StorySettings
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private const val VIEW_GROUP = "android.view.ViewGroup"
private const val VIEW_GONE = 8

val hideStoryTray = patch(InstagramPatchStrings.hideStoryTray) {
    description(InstagramPatchStrings.hideStoryTrayDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionStories, StorySettings.hideStoryTray))

    execute {
        val trayContainer = resources.id("id", "reels_tray_container")?.toLong() ?: error("id/reels_tray_container missing")
        // The Home header's view holder looks the tray up once, when the header is inflated.
        val trayLookup = method("storyTrayHolder") {
            name("<init>")
            params(VIEW_GROUP)
            literals(trayContainer)
        }.point { literal(trayContainer) }
            .next { checkCast(VIEW_GROUP) }
            .captureAs("tray")
        gate(StorySettings.hideStoryTray) {
            trayLookup.after {
                capture("tray").callVirtual("android.view.View", "setVisibility", "(I)V", int(VIEW_GONE))
            }
        }
    }
}
