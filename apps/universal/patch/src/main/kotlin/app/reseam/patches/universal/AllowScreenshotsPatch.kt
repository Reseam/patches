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
    val addFlags = static("addFlags", WINDOW, Type.Int)
    val setFlags = static("setFlags", WINDOW, Type.Int, Type.Int)
    val setAttributes = static("setAttributes", WINDOW, "android.view.WindowManager\$LayoutParams")
    val addWindow = static("addView", WINDOW_MANAGER, Type.View, LAYOUT_PARAMS)
    val updateWindow = static("updateViewLayout", WINDOW_MANAGER, Type.View, LAYOUT_PARAMS)
    val addView = static("addView", VIEW_MANAGER, Type.View, LAYOUT_PARAMS)
    val updateViewLayout = static("updateViewLayout", VIEW_MANAGER, Type.View, LAYOUT_PARAMS)
    val setSecure = static("setSecure", "android.view.SurfaceView", Type.Boolean)
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
