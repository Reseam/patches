// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.patch

val enableDebugging = patch("Enable debugging") {
    description("Marks the app debuggable, so adb can run-as it and attach debuggers without root.")

    execute {
        manifest.edit {
            val application = findByTag("application").firstOrNull() ?: error("manifest has no <application> element")
            application["android:debuggable"] = "true"
        }
    }
}
