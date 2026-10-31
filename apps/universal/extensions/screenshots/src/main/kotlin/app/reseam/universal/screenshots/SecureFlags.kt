// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

@file:JvmName("SecureFlags")

package app.reseam.universal.screenshots

import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.view.Window
import android.view.WindowManager
import app.reseam.runtime.Export

@Export
fun addFlags(window: Window, flags: Int) =
    window.addFlags(flags and WindowManager.LayoutParams.FLAG_SECURE.inv())

@Export
fun setFlags(window: Window, flags: Int, mask: Int) =
    window.setFlags(flags and WindowManager.LayoutParams.FLAG_SECURE.inv(), mask)

@Export
fun setAttributes(window: Window, params: WindowManager.LayoutParams?) {
    window.attributes = cleared(params)
}

@Export(name = "addWindow")
fun addView(manager: WindowManager, view: View, params: ViewGroup.LayoutParams?) =
    manager.addView(view, cleared(params))

@Export(name = "updateWindow")
fun updateViewLayout(manager: WindowManager, view: View, params: ViewGroup.LayoutParams?) =
    manager.updateViewLayout(view, cleared(params))

@Export
fun addView(manager: ViewManager, view: View, params: ViewGroup.LayoutParams?) =
    manager.addView(view, cleared(params))

@Export
fun updateViewLayout(manager: ViewManager, view: View, params: ViewGroup.LayoutParams?) =
    manager.updateViewLayout(view, cleared(params))

@Export fun setSecure(view: SurfaceView, secure: Boolean) = view.setSecure(false)

private fun <T : ViewGroup.LayoutParams> cleared(params: T?): T? = params.also {
    if (it is WindowManager.LayoutParams)
        it.flags = it.flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
}
