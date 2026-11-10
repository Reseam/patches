// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.patches.universal

import app.reseam.patch.patch

val enableDebugging = patch(UniversalPatchStrings.enableDebugging) {
    description(UniversalPatchStrings.enableDebuggingDescription)

    execute {
        manifest.edit {
            val application = findByTag("application").firstOrNull() ?: error("manifest has no <application> element")
            application["android:debuggable"] = "true"
        }
    }
}
