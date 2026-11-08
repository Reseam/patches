// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.Type
import app.reseam.patch.methodTarget

const val MAIN_ACTIVITY = "com.x.android.main.MainActivity"

val mainActivityOnCreate = mainActivityMethod("onCreate", "Landroid/os/Bundle;")

val mainActivityAttachBaseContext = mainActivityMethod("attachBaseContext", Type.Context)

private fun mainActivityMethod(name: String, vararg parameters: String) = methodTarget("mainActivity${name.replaceFirstChar(Char::uppercase)}") {
    val activity = bytecode.findClass(MAIN_ACTIVITY) ?: error("$MAIN_ACTIVITY missing")
    activity.virtualMethods.single { it.name == name && it.parameterTypes == parameters.toList() }
}
