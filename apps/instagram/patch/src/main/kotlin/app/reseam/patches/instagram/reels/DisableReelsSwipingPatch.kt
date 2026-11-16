// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.reels

import app.reseam.instagram.reels.bindings.ReelsSwiping
import app.reseam.instagram.settings.InstagramStrings
import app.reseam.instagram.settings.PlaybackSettings
import app.reseam.patch.CodeScope
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val disableReelsSwiping = patch(InstagramPatchStrings.disableReelsSwiping) {
    description(InstagramPatchStrings.disableReelsSwipingDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionPlayback, PlaybackSettings.disableReelsSwiping, PlaybackSettings.swipeInReelsTab))

    execute {
        val reelsPager = resources.id("id", "clips_viewer_view_pager")?.toInt() ?: error("id/clips_viewer_view_pager missing")
        gate(PlaybackSettings.disableReelsSwiping) {
            clipsViewerViewCreated.before {
                ReelsSwiping.viewerCreated(param(0), thisObject.callVirtual(clipsViewer.descriptor, "getModuleName", "()Ljava/lang/String;"))
            }
            // The viewer turns swiping back on after sheets and gestures, so every change on its pager is held off.
            viewPager.method("setAdapter").before {
                onLockedPager(reelsPager) { thisObject.callVirtual(VIEW_PAGER, "setUserInputEnabled", "(Z)V", bool(false)) }
            }
            viewPager.method("setUserInputEnabled").before {
                onLockedPager(reelsPager) { param(0).assign(bool(false)) }
            }
        }
    }
}

private const val VIEW_PAGER = "androidx.viewpager2.widget.ViewPager2"

private val viewPager = klass(VIEW_PAGER)

private val clipsViewer = klass("clipsViewer") { strings("ClipsViewerFragment.onViewCreated:viewPagerSetup") }

private val clipsViewerViewCreated = clipsViewer.method("onViewCreated") { params("android.view.View", "android.os.Bundle") }

private fun CodeScope.onLockedPager(id: Int, block: CodeScope.() -> Unit) =
    whenEqual(thisObject.callVirtual("android.view.View", "getId", "()I"), int(id)) {
        whenTrue(ReelsSwiping.locked(thisObject), block)
    }
