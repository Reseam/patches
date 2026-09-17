// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.patch
import app.reseam.patches.youtube.core.YOUTUBE

val seekbar = patch("Seekbar") {
    description(
        "Adds options to hide the seekbar, use slide or tap to seek, and disable precise seeking.",
    )
    compatibleWith(YOUTUBE)
    dependsOn(hideSeekbar, enableSlideToSeek, enableTapToSeek, disablePreciseSeekingGesture)
}
