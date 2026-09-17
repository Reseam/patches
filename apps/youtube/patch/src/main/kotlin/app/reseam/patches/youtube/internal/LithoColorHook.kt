// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.internal

import app.reseam.patch.ExtMethod
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patches.youtube.core.YOUTUBE

private const val PAINT = "android.graphics.Paint"

private val colorHooks = mutableListOf<ExtMethod>()

/** Lets theme patches share the one Paint.setColor seam used by Litho components. */
fun registerLithoColorHook(hook: ExtMethod) {
    colorHooks += hook
}

val lithoColorHook = patch {
    compatibleWith(YOUTUBE)

    afterDependents {
        val hooks = colorHooks.toList()
        colorHooks.clear()
        if (hooks.isEmpty()) return@afterDependents

        lithoOnBoundsChange
            .point("lithoPaintSetColor") {
                invokeVirtual {
                    owner(PAINT)
                    name("setColor")
                    params(Type.Int)
                    returns(Type.Void)
                }
            }
            .captureArgumentAs("color", 1)
            .before {
                var color = capture("color")
                hooks.forEach { hook -> color = call(hook, color) }
                capture("color").assign(color)
            }
    }
}

private val lithoOnBoundsChange = method("lithoOnBoundsChange") {
    flags(AccessFlags.PROTECTED or AccessFlags.FINAL)
    params("android.graphics.Rect")
    returns(Type.Void)
    calls { owner("Landroid/graphics/Paint;"); name("setColor"); params(Type.Int); returns(Type.Void) }
}
