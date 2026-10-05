// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.ExtClass
import app.reseam.patch.Type
import app.reseam.patch.patch

private const val WINDOW = "android.view.Window"
private const val WINDOW_MANAGER = "android.view.WindowManager"
private const val VIEW_MANAGER = "android.view.ViewManager"
private const val LAYOUT_PARAMS = "android.view.ViewGroup\$LayoutParams"

private object SecureFlags : ExtClass("app.reseam.universal.screenshots.SecureFlags") {
    val addFlags by static(WINDOW, Type.Int)
    val setFlags by static(WINDOW, Type.Int, Type.Int)
    val setAttributes by static(WINDOW, "android.view.WindowManager\$LayoutParams")
    val addWindow by static(WINDOW_MANAGER, Type.View, LAYOUT_PARAMS, name = "addView")
    val updateWindow by static(WINDOW_MANAGER, Type.View, LAYOUT_PARAMS, name = "updateViewLayout")
    val addView by static(VIEW_MANAGER, Type.View, LAYOUT_PARAMS)
    val updateViewLayout by static(VIEW_MANAGER, Type.View, LAYOUT_PARAMS)
    val setSecure by static("android.view.SurfaceView", Type.Boolean)
}

val allowScreenshots = patch("Allow screenshots") {
    description("Allows screenshots and screen recording on screens the app protects.")

    execute {
        val redirected = with(SecureFlags) {
            bytecode.redirectInstanceCalls(addFlags, setFlags, setAttributes, addWindow, updateWindow, addView, updateViewLayout, setSecure)
        }
        log.info("Cleared the secure flag at $redirected call sites")
    }
}
