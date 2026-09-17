// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

private const val DURATION = "j\$.time.Duration"

val disableDoubleTapActions = patch("Disable double tap actions") {
    description("Double tapping the player seeks by the usual amount instead of jumping to a chapter.")
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Controls, "Double tap", YouTubeSettings.disableChapterSkipDoubleTap))

    execute {
        // The flag is read twice: once as it is recorded, once as the seek source is chosen.
        doubleTapInfoConstructor.before(YouTubeSettings.disableChapterSkipDoubleTap) {
            param(2).assign(bool(false))
        }
        doubleTapSeekSource.before(YouTubeSettings.disableChapterSkipDoubleTap) {
            param(0).assign(bool(false))
        }
    }
}

// What the player records about a double tap: where it landed, which half, whether a chapter
// boundary is in reach, and how long the gesture lasted.
val doubleTapInfo = klass("doubleTapInfo") {
    hasInstanceField("android.view.MotionEvent")
    hasInstanceField(DURATION)
}

val doubleTapInfoConstructor = method("doubleTapInfoConstructor") {
    inClass(doubleTapInfo)
    flags(AccessFlags.CONSTRUCTOR)
    params("android.view.MotionEvent", Type.Int, Type.Boolean, DURATION)
}

// The only method on the record that takes the chapter flag and answers with a seek source.
val doubleTapSeekSource = method("doubleTapSeekSource") {
    inClass(doubleTapInfo)
    params(Type.Boolean)
}
