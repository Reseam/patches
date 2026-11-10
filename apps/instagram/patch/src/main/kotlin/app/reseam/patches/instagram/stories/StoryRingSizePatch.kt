// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.instagram.stories

import app.reseam.instagram.settings.InstagramStrings
import app.reseam.instagram.settings.StorySettings
import app.reseam.instagram.stories.bindings.StoryRings
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.InstagramPatchStrings
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

private const val AVATAR_TO_ITEM = 0x3f495e9eL // 0.7866f
private const val TWO = 0x40000000L // 2.0f

val storyRingSize = patch(InstagramPatchStrings.storyRingSize) {
    description(InstagramPatchStrings.storyRingSizeDescription)
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section(InstagramStrings.sectionStories, StorySettings.ringSize))

    execute {
        trayItemSize.before { capture("itemSize").assign(StoryRings.scaled(capture("itemSize"))) }
    }
}

// The tray fits a fixed number of items to the screen width, clamps the item size, then derives the
// avatar, ring and spacing from it as ratios. The first ring inset (size minus twice an inset)
// follows the clamp on every branch.
private val trayItemSize = method("storyTrayDimensions") { name("<init>"); literals(AVATAR_TO_ITEM) }
    .point { literal(TWO) }
    .previous { invokeStatic { params(Type.Context, Type.Float); returns(Type.Float) } }
    .captureArgumentAs("itemSize", 1, Type.Float)
