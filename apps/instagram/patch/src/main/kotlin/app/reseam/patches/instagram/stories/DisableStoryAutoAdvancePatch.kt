// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.instagram.stories

import app.reseam.patch.Type
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.instagram.core.INSTAGRAM
import app.reseam.patches.instagram.core.StorySettings
import app.reseam.patches.instagram.core.instagramSettings
import app.reseam.patches.instagram.core.signatureCheck

val disableStoryAutoAdvance = patch("Disable story auto-advance") {
    description("Keeps a story on screen after it finishes, until you tap to move on.")
    compatibleWith(INSTAGRAM)
    dependsOn(signatureCheck)
    settings(instagramSettings, section("Stories", StorySettings.disableAutoAdvance))

    execute {
        gate(StorySettings.disableAutoAdvance) {
            storyItemFinished.alwaysReturn()
        }
    }
}

// The photo timer and video completion both report a finished item here. The other (Object)V
// viewer callback loads "sponsored" and not "userSession".
private val storyItemFinished = method("storyItemFinished") {
    inClass(klass("instagram.features.stories.fragment.ReelViewerFragment"))
    strings("userSession")
    params(Type.Object)
    returns(Type.Void)
}
