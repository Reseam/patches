// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.media

import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.Opcode
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.settings.section
import app.reseam.patch.settings.valueOf
import app.reseam.patches.x.XPatchStrings
import app.reseam.patches.x.core.X
import app.reseam.patches.x.core.XSettingsPages
import app.reseam.patches.x.core.xSettings
import app.reseam.x.settings.XSettings
import app.reseam.x.settings.XStrings

val doubleTapSeek = patch(XPatchStrings.doubleTapSeek) {
    description(XPatchStrings.doubleTapSeekDescription)
    compatibleWith(X)
    settings(xSettings, section(XSettingsPages.Media, XStrings.sectionVideo, XSettings.doubleTapSeek))

    execute {
        // The video player's double-tap labels read the seek step, which a static initializer builds from whole seconds.
        val skipBackLabel = resources.id("string", "immersive_video_skip_back_action_a11y")?.toLong()
            ?: error("string/immersive_video_skip_back_action_a11y missing")
        val seekStep = method("doubleTapSeekLabels") { literals(skipBackLabel) }
            .point { opcode(Opcode.SGET_WIDE) }
            .field("doubleTapSeekStep")
        klass(seekStep.owner).method("<clinit>")
            .point { field { owner(seekStep.owner); name(seekStep.name) } }
            .previous { invokeStatic { params(Type.Int, "Lkotlin/time/DurationUnit;") } }
            .captureArgumentAs("seconds", 0)
            .before {
                capture("seconds").assign(
                    callStatic("java.lang.Integer", "parseInt", "(Ljava/lang/String;)I", valueOf(XSettings.doubleTapSeek)),
                )
            }
    }
}
