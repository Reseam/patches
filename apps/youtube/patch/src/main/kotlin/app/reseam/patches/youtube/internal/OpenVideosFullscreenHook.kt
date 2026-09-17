// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.settings.after
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.literal
import app.reseam.patch.dex.opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.youTubeSettings

private const val OPTIONAL = "j$.util.Optional"

/** Shared by the public fullscreen patch; hidden so it is not offered independently. */
val openVideosFullscreenHook = patch {
    compatibleWith(YOUTUBE)
    dependsOn(youTubeSettings)

    execute {
        val target = method("openVideosFullscreenPortrait") {
            returns(Type.Void)
            paramCount(2)
            param(1, OPTIONAL)
            literals(45666112L)
            custom { parameterTypes.firstOrNull()?.startsWith("L") == true }
        }

        // The feature literal identifies the existing conditional; the nearest preceding
        // move-result is the original decision value and is safe to rewrite after the read.
        target.point("fullscreenConditional") { literal(45666112L) }
            .previous { opcode(Opcode.MOVE_RESULT) }
            .captureAs("fullscreenConditional", Type.Boolean)
            .after(YouTubeSettings.openVideosFullscreen) {
                capture("fullscreenConditional").assign(bool(false))
            }
        target.point("fullscreenFeatureFlag") { literal(45666112L) }
            .next { invokeVirtual { returns(Type.Boolean) } }
            .next { opcode(Opcode.MOVE_RESULT) }
            .captureAs("featureFlag", Type.Boolean).after {
            capture("featureFlag").assign(bool(false))
        }
    }
}
