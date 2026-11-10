// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.explore

import app.reseam.instagram.settings.AppearanceSettings
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private const val RECYCLER_VIEW = "androidx.recyclerview.widget.RecyclerView"
private const val VIEW_GONE = 8

val hideExploreGrid = patch(InstagramPatchStrings.hideExploreGrid) {
    description(InstagramPatchStrings.hideExploreGridDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionAppearance, AppearanceSettings.hideExploreGrid))

    execute {
        gate(AppearanceSettings.hideExploreGrid) {
            exploreGridSetup.after {
                capture("grid").callVirtual("android.view.View", "setVisibility", "(I)V", int(VIEW_GONE))
            }
        }
    }
}

private val exploreFragment = klass("exploreFragment") { strings("ExploreFragment.setupGrid") }

private val exploreGridSetup = method("exploreViewCreated") {
    inClass(exploreFragment)
    name("onViewCreated")
}.point { invokeVirtual { owner(RECYCLER_VIEW); name("setItemAnimator") } }
    .captureArgumentAs("grid", 0, RECYCLER_VIEW)
