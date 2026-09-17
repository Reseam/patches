// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.interaction

import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.classTarget
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.reserveLocal
import app.reseam.patch.settings.section
import app.reseam.patch.settings.whenEnabled
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings

private const val POINT = "android.graphics.Point"
private const val OPTIONAL = "j\$.util.Optional"

val enableTapToSeek = patch {
    compatibleWith(YOUTUBE)
    settings(youTubeSettings, section(YouTubeSettingsPages.Controls, "Seekbar", YouTubeSettings.tapToSeek))

    execute {
        val hitTest = tapToSeekHandler.point("seekbarHitTest") {
            invokeVirtual { params(Type.Float, Type.Float); returns(Type.Boolean) }
        }
        val original = hitTest.previous { invokeVirtual { params(Type.Int); returns(Type.Void) } }.callee()
        val replacement = hitTest.next { invokeVirtual { params(Type.Int); returns(Type.Void) } }.callee()

        // The tap stores its position as `Optional.of(new Point(x, y))`; the call that follows runs
        // on the seekbar itself, which is where the seek methods are invoked with that x.
        val tapPoint = tapToSeekMethod.point("tapToSeekPoint") {
            invokeDirect { owner(POINT); name("<init>"); params(Type.Int, Type.Int) }
        }.captureArgumentAs("x", 1, Type.Int)
        val tapX = tapToSeekMethod.reserveLocal("tapX", Type.Int)
        tapPoint.after { local(tapX).assign(capture("x")) }

        tapPoint
            .next { invokeStatic { owner(OPTIONAL); name("of") } }
            .next { opcode(Opcode.MOVE_RESULT_OBJECT) }
            .next { opcode(Opcode.IPUT_OBJECT); field { type(OPTIONAL) } }
            .next { invokeVirtual { } }
            .captureArgumentAs("seekbar", 0)
            .after {
                whenEnabled(YouTubeSettings.tapToSeek) {
                    capture("seekbar").call(original, local(tapX))
                    capture("seekbar").call(replacement, local(tapX))
                }
            }
    }
}

// Start from the resource-labelled seekbar, then use its inherited touch handler.
private val seekbarClass = classTarget("tapSeekbar") {
    bytecode.findClass(seekbarOnDraw.owner) ?: error("The seekbar class is missing")
}
private val baseSeekbarClass = classTarget("baseSeekbar") {
    bytecode.findClass(seekbarClass.classDef.superclass!!) ?: error("The base seekbar class is missing")
}
private val tapToSeekHandler = baseSeekbarClass.method("onTouchEvent", inherited = true) {
    params("android.view.MotionEvent")
}
private val tapToSeekMethod = seekbarClass.method("onTouchEvent") {
    params("android.view.MotionEvent")
}
