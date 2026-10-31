// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.patch
import app.reseam.universal.screenshots.bindings.SecureFlags

private const val WINDOW = "android.view.Window"
private const val WINDOW_MANAGER = "android.view.WindowManager"
private const val VIEW_MANAGER = "android.view.ViewManager"
private const val LAYOUT_PARAMS = "android.view.ViewGroup\$LayoutParams"
val allowScreenshots = patch("Allow screenshots") {
    description("Allows screenshots and screen recording on screens the app protects.")

    execute {
        val redirected = with(SecureFlags) {
            bytecode.redirectInstanceCalls(addFlags, setFlags, setAttributes, addWindow, updateWindow, addView, updateViewLayout, setSecure)
        }
        log.info("Cleared the secure flag at $redirected call sites")
    }
}
