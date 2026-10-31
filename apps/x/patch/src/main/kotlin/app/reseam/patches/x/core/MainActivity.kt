// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.x.core

import app.reseam.patch.methodTarget

const val MAIN_ACTIVITY = "com.x.android.main.MainActivity"

val mainActivityOnCreate = methodTarget("mainActivityOnCreate") {
    val activity = bytecode.findClass(MAIN_ACTIVITY) ?: error("$MAIN_ACTIVITY missing")
    activity.virtualMethods.single { it.name == "onCreate" && it.parameterTypes == listOf("Landroid/os/Bundle;") }
}
