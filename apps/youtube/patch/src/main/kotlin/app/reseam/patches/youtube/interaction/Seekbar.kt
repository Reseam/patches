// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.patch
import app.reseam.patches.youtube.YouTubePatchStrings
import app.reseam.patches.youtube.core.YOUTUBE

val seekbar = patch(YouTubePatchStrings.seekbar) {
    description(YouTubePatchStrings.seekbarDescription)
    compatibleWith(YOUTUBE)
    dependsOn(hideSeekbar, enableSlideToSeek, enableTapToSeek, scrubbingPreview, disablePreciseSeekingGesture)
}
