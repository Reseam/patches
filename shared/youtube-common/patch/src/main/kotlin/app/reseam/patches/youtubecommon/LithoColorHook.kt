// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.youtubecommon

import app.reseam.patch.ExtMethod
import app.reseam.patch.RunList
import app.reseam.patch.Type
import app.reseam.patch.before
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.method
import app.reseam.patch.patch
import app.reseam.patch.point

private const val PAINT = "android.graphics.Paint"

private val colorHooks = RunList<ExtMethod>()

/** Lets theme patches share the one Paint.setColor seam used by Litho components. */
fun registerLithoColorHook(hook: ExtMethod) {
    colorHooks.add(hook)
}

val lithoColorHook = patch {
    afterDependents {
        val hooks = colorHooks.values
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
                capture("color").assign(hooks.fold(capture("color")) { color, hook -> call(hook, color) })
            }
    }
}

private val lithoOnBoundsChange = method("lithoOnBoundsChange") {
    flags(AccessFlags.PROTECTED or AccessFlags.FINAL)
    params("android.graphics.Rect")
    returns(Type.Void)
    calls { owner("Landroid/graphics/Paint;"); name("setColor"); params(Type.Int); returns(Type.Void) }
}
